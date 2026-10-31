package com.shipping.wars.shipper.controller;

import com.shipping.wars.shipper.entity.AuctionActivityLog;
import com.shipping.wars.shipper.service.AuctionActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuctionActivityController {

    @Autowired
    private AuctionActivityService auctionActivityService;

    @GetMapping("/rfqs/{refId}/activity")
    public ResponseEntity<List<AuctionActivityLog>> getActivityByRFQReferenceId(@PathVariable String refId) {
        return ResponseEntity.ok(auctionActivityService.getActivityByRFQReferenceId(refId));
    }
}
