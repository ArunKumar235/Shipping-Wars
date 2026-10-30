package com.shipping.wars.shipper.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auction_config")
@Getter
@Setter
public class AuctionConfig {
    
    @Id
    @Column(name = "rfq_id")
    private Long rfqId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "rfq_id")
    private RFQ rfq;

    @Min(value = 0)
    private Integer triggerWindowMinutes;

    @Min(value = 0)
    private Integer extensionMinutes;

    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;
}
