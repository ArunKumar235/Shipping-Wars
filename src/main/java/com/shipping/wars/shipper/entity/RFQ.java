package com.shipping.wars.shipper.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(
    name = "rfq",
    check = {
        @CheckConstraint(name = "chk_rfq_bid_close_time", constraint = "bid_start_time < bid_close_time"),
        @CheckConstraint(name = "chk_rfq_forced_close_time", constraint = "bid_close_time <= forced_close_time"),
        @CheckConstraint(name = "chk_rfq_actual_close_time", constraint = "bid_close_time <= actual_close_time AND actual_close_time <= forced_close_time")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQ {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String referenceId;

    private String name;

    private String description;

    private Instant bidStartTime;
    
    private Instant bidCloseTime; // intended close time

    private Instant actualCloseTime;

    private Instant forcedCloseTime;

    private Instant pickUpDate;

    @OneToOne(
        mappedBy = "rfq",
        cascade = CascadeType.ALL, 
        orphanRemoval = true, 
        fetch = FetchType.LAZY
    )
    private AuctionConfig auctionConfig;

    @OneToMany(
        mappedBy = "rfq",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<BID> bids = new ArrayList<>();

    @OneToMany(
        mappedBy = "rfq",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<AuctionActivityLog> logs = new ArrayList<>();

    @CreationTimestamp 
    private Instant createdAt;
}
