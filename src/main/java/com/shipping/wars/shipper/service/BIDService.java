package com.shipping.wars.shipper.service;

import com.shipping.wars.shipper.dto.CreateBidRequest;
import com.shipping.wars.shipper.entity.*;
import com.shipping.wars.shipper.repository.BIDRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;

import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class BIDService {

    @Autowired
    private BIDRepository bidRepository;

    @Autowired
    private RFQService rfqService;

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private AuctionActivityService auctionActivityService;

    private static final Comparator<BID> BID_RANKING_COMPARATOR =
            Comparator.comparing(BID::getTotalPrice)
                    .thenComparing(BID::getTransitDays)
                    .thenComparing(BID::getSubmittedAt, Comparator.reverseOrder()) // recent first
                    .thenComparing(BID::getSupplierId);

    public List<BID> getBidsByRFQReferenceId(String refId) {
        return bidRepository.findByRfqReferenceIdWithSupplier(refId);
    }

    @Retryable(
            includes = OptimisticLockingFailureException.class,
            maxRetries = 3,
            delay = 100
    )
    @Transactional
    public BID createBid(String refId, CreateBidRequest bidRequest) {
        // Verify RFQ and participate in optimistic locking
        RFQ rfq = rfqService.findForBidSubmission(refId);

        // Verify that the Supplier exists
        Supplier supplier = supplierService.getSupplierById(bidRequest.supplierId());

        // Verify time of bid is before rfq.bidCloseTime
        Instant now = Instant.now();
        verifyBidTiming(now, rfq.getBidCloseTime());

        // Validate against previous bid from same supplier
        BigDecimal newTotal = calculateTotal(bidRequest);

        validateBidAgainstPrevious(refId, supplier.getId(), newTotal);

        List<BID> previousRankings = bidRepository.findLatestBidsByRFQ(rfq.getId());

        // Create and save the BID
        BID newBid = new BID();
        newBid.setRfq(rfq);
        newBid.setSupplier(supplier);
        newBid.setCarrierName(bidRequest.carrierName());
        newBid.setFreightCharge(bidRequest.freightCharge());
        newBid.setOriginCharge(bidRequest.originCharge());
        newBid.setDestinationCharge(bidRequest.destinationCharge());
        newBid.setTotalPrice(newTotal);
        newBid.setTransitDays(bidRequest.transitDays());
        newBid.setQuoteValidity(bidRequest.quoteValidity());
        newBid.setSubmittedAt(now);

        BID savedBid = bidRepository.saveAndFlush(newBid);

        // Log bid placement
        auctionActivityService.logBidPlaced(rfq, savedBid);

        List<BID> currentRankings = bidRepository.findLatestBidsByRFQ(rfq.getId());

        runExtensionLogic(rfq, now, savedBid, previousRankings, currentRankings);

        return savedBid;
    }

    private BigDecimal calculateTotal(CreateBidRequest bidRequest) {
        return bidRequest.freightCharge()
                .add(bidRequest.originCharge())
                .add(bidRequest.destinationCharge());
    }

    private void verifyBidTiming(Instant now, Instant closeTime) {
        if(!now.isBefore(closeTime)){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot place bid: auction has already closed at " + closeTime);
        }
    }

    private void validateBidAgainstPrevious(String refId, Long supplierId, BigDecimal newTotal) {
        List<BID> prior = bidRepository.findLatestByRfqAndSupplier(refId, supplierId);
        if (!prior.isEmpty()) {
            BID lastBid = prior.getFirst();
            boolean betterPrice = newTotal.compareTo(lastBid.getTotalPrice()) < 0;
            if (!betterPrice) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New bid must improve total price compared to previous bid.");
            }
        }
    }

    private void runExtensionLogic(RFQ rfq, Instant now, BID triggeredBid, List<BID> previousBids, List<BID> currentBids) {
        TriggerType triggerType = rfq.getAuctionConfig().getTriggerType();

        switch (triggerType) {
            case ANY_BID -> {
                extendAuctionIfEligible(rfq, now, triggeredBid, "ANY_BID");
            }

            case ANY_RANK_CHANGE -> {
                if (hasRankChanged(previousBids, currentBids)) {
                    extendAuctionIfEligible(rfq, now, triggeredBid, "ANY_RANK_CHANGE");
                }
            }

            case L1_CHANGE -> {
                if (hasL1Changed(previousBids, currentBids)) {
                    extendAuctionIfEligible(rfq, now, triggeredBid, "L1_CHANGE");
                }
            }
        }
    }

    private void extendAuctionIfEligible(RFQ rfq, Instant now, BID triggeredBid, String triggerReason) {
        AuctionConfig config = rfq.getAuctionConfig();
        Instant closeTime = rfq.getBidCloseTime();
        Instant forcedCloseTime = rfq.getForcedCloseTime();

        if (!closeTime.isBefore(forcedCloseTime)) return; // already at cap

        Instant windowStart = closeTime.minus(config.getTriggerWindowMinutes(), ChronoUnit.MINUTES);
        boolean inWindow = !now.isBefore(windowStart) && now.isBefore(closeTime);

        if (!inWindow) return;

        Instant newCloseTime = closeTime.plus(config.getExtensionMinutes(), ChronoUnit.MINUTES);
        if (newCloseTime.isAfter(forcedCloseTime)) {
            newCloseTime = forcedCloseTime;
        }

        rfq.setBidCloseTime(newCloseTime);

        // Log the extension with the old/new close times and the reason
        auctionActivityService.logTimeExtended(rfq, triggeredBid, closeTime, newCloseTime, triggerReason);
    }

    private Map<Long, Integer> calculateRanks(List<BID> bids) {
        List<BID> rankedBids = bids.stream()
                .sorted(BID_RANKING_COMPARATOR)
                .toList();

        Map<Long, Integer> ranks = new HashMap<>();

        for (int i = 0; i < rankedBids.size(); i++) {
            ranks.put(rankedBids.get(i).getSupplierId(), i + 1);
        }
        return ranks;
    }

    private boolean hasRankChanged(List<BID> previousBids, List<BID> currentBids) {
        return !Objects.equals(calculateRanks(previousBids), calculateRanks(currentBids));
    }

    private Long findL1Supplier(List<BID> bids) {
        return bids.stream()
                .min(BID_RANKING_COMPARATOR)
                .map(BID::getSupplierId)
                .orElse(null);
    }

    private boolean hasL1Changed(List<BID> previousBids, List<BID> currentBids) {
        Long previousL1 = findL1Supplier(previousBids);
        Long currentL1 = findL1Supplier(currentBids);

        return !Objects.equals(previousL1, currentL1);
    }
}
