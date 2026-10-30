package com.shipping.wars.shipper.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "bid")
@Getter 
@Setter 
public class BID {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id", nullable = false)
    private RFQ rfq;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal freightCharge;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal originCharge;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal destinationCharge;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPrice;

    @Min(1)
    @Column(nullable = false)
    private Integer transitTimeDays;

    @NotNull
    @Column(nullable = false)
    private LocalDate quoteValidity;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    @PreUpdate
    private void calculateTotalPrice() {
        BigDecimal freight = (freightCharge != null) ? freightCharge : BigDecimal.ZERO;
        BigDecimal origin = (originCharge != null) ? originCharge : BigDecimal.ZERO;
        BigDecimal destination = (destinationCharge != null) ? destinationCharge : BigDecimal.ZERO;
        
        this.totalPrice = freight.add(origin).add(destination);
    }
}
