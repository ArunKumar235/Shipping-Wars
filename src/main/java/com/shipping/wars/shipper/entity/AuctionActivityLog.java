package com.shipping.wars.shipper.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "auction_activity_log")
@Getter 
@Setter 
public class AuctionActivityLog {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    @JsonIgnore
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bid_id")
    private BID bid;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private Instant oldCloseTime;

    private Instant newCloseTime;

    private String reason;

    @CreationTimestamp 
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
