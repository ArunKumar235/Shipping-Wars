package com.shipping.wars.shipper.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RFQ {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rfq_seq_gen")
    @SequenceGenerator(name = "rfq_seq_gen", sequenceName = "rfq_id_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String referenceId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private Instant bidStartTime;

    @Column(nullable = false)
    private Instant bidCloseTime;

    @Column(nullable = false)
    private Instant forcedCloseTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", timezone = "UTC")
    private LocalDate pickUpDate;

    @OneToOne(
            mappedBy = "rfq",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private AuctionConfig auctionConfig;

    @OneToMany(
            mappedBy = "rfq",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<BID> bids = new ArrayList<>();

    @OneToMany(
            mappedBy = "rfq",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnore
    private List<AuctionActivityLog> logs = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;

    @Version
    private Long version;

    @PrePersist
    public void generateReferenceId() {
        if (this.id != null) {
            this.referenceId = String.format("RFQ-%04d", this.id);
        }
    }
}
    