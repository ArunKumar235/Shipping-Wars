package com.shipping.wars.shipper.controller;

import com.shipping.wars.shipper.dto.CreateBidRequest;
import com.shipping.wars.shipper.entity.BID;
import com.shipping.wars.shipper.service.BIDService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BIDController {

    @Autowired
    private BIDService bidService;

    @GetMapping("/rfqs/{refId}/bids")
    public ResponseEntity<List<BID>> getBidsByRFQReferenceId(@PathVariable String refId) {
        return ResponseEntity.ok(bidService.getBidsByRFQReferenceId(refId));
    }

    @PostMapping("/rfqs/{refId}/bids")
    public ResponseEntity<BID> createBid(@PathVariable String refId, @RequestBody @Valid CreateBidRequest bid) {
        return ResponseEntity.ok(bidService.createBid(refId, bid));
    }

}
