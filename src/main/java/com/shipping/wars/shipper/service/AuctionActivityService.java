package com.shipping.wars.shipper.service;

import com.shipping.wars.shipper.entity.AuctionActivityLog;
import com.shipping.wars.shipper.entity.BID;
import com.shipping.wars.shipper.entity.EventType;
import com.shipping.wars.shipper.entity.RFQ;
import com.shipping.wars.shipper.repository.AuctionActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AuctionActivityService {

    @Autowired
    private AuctionActivityRepository auctionActivityRepository;

    public List<AuctionActivityLog> getActivityByRFQReferenceId(String refId) {
        return auctionActivityRepository.findByRfqReferenceIdWithBid(refId);
    }

    public void logBidPlaced(RFQ rfq, BID bid) {
        AuctionActivityLog log = new AuctionActivityLog();
        log.setRfq(rfq);
        log.setBid(bid);
        log.setEventType(EventType.BID_PLACED);
        log.setReason("Bid placed by supplier " + bid.getSupplier().getName()
                + " — total: " + bid.getTotalPrice()
                + ", transit days: " + bid.getTransitDays());
        auctionActivityRepository.save(log);
    }

    public void logTimeExtended(RFQ rfq, BID bid, Instant oldClose, Instant newClose, String triggerReason) {
        AuctionActivityLog log = new AuctionActivityLog();
        log.setRfq(rfq);
        log.setBid(bid);
        log.setEventType(EventType.TIME_EXTENDED);
        log.setOldCloseTime(oldClose);
        log.setNewCloseTime(newClose);
        log.setReason("Auction extended due to " + triggerReason
                + " — from " + oldClose + " to " + newClose);
        auctionActivityRepository.save(log);
    }
}
