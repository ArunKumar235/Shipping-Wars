package com.shipping.wars.shipper.controller;

import com.shipping.wars.shipper.dto.CreateRfqRequest;
import com.shipping.wars.shipper.entity.RFQ;
import com.shipping.wars.shipper.service.RFQService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/rfqs")
public class RFQController {

    @Autowired
    private RFQService rfqService;

    @GetMapping
    public ResponseEntity<List<RFQ>> getAllRFQs() {
        return ResponseEntity.ok(rfqService.getAllRFQs());
    }

    @GetMapping("/active")
    public ResponseEntity<List<RFQ>> getActiveRFQs() {
        return ResponseEntity.ok(rfqService.getActiveRFQs());
    }

    @GetMapping("/{refId}")
    public ResponseEntity<RFQ> getRFQByReferenceId(@PathVariable String refId) {
        return ResponseEntity.ok(rfqService.getRFQByReferenceId(refId));
    }

    @PostMapping
    public ResponseEntity<RFQ> createRFQ(@Valid @RequestBody CreateRfqRequest rfq) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rfqService.createRFQ(rfq));
    }

}
