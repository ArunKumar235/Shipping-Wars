package com.shipping.wars.shipper.service;

import com.shipping.wars.shipper.dto.CreateRfqRequest;
import com.shipping.wars.shipper.entity.AuctionConfig;
import com.shipping.wars.shipper.entity.RFQ;
import com.shipping.wars.shipper.repository.RFQRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class RFQService {

    @Autowired
    private RFQRepository rfqRepository;

    public List<RFQ> getAllRFQs() {
        return rfqRepository.findAll();
    }

    public List<RFQ> getActiveRFQs() {
        return rfqRepository.findActiveRFQs(Instant.now());
    }

    public RFQ createRFQ(CreateRfqRequest rfq) {
        RFQ newRfq = new RFQ();
        newRfq.setName(rfq.name());
        newRfq.setDescription(rfq.description());
        newRfq.setBidStartTime(rfq.bidStartTime());
        newRfq.setBidCloseTime(rfq.bidCloseTime());
        newRfq.setForcedCloseTime(rfq.forcedCloseTime());
        newRfq.setPickUpDate(rfq.pickUpDate());

        AuctionConfig auctionConfig = new AuctionConfig();
        auctionConfig.setRfq(newRfq);
        auctionConfig.setTriggerWindowMinutes(rfq.auctionConfig().triggerWindowInMinutes());
        auctionConfig.setExtensionMinutes(rfq.auctionConfig().extensionMinutes());
        auctionConfig.setTriggerType(rfq.auctionConfig().triggerType());

        newRfq.setAuctionConfig(auctionConfig);
        return rfqRepository.save(newRfq);
    }

    public RFQ getRFQByReferenceId(String refId) {
        return rfqRepository.findByReferenceId(refId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RFQ not found with reference ID: " + refId));
    }

    public RFQ findForBidSubmission(String refId) {
        return rfqRepository.findForBidSubmission(refId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RFQ not found with reference ID: " + refId));
    }

    public RFQ update(RFQ rfq) {
        return rfqRepository.save(rfq);
    }
}
