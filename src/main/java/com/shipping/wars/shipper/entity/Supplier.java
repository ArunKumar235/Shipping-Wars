package com.shipping.wars.shipper.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Table(name = "supplier")
@Getter 
@Setter 
public class Supplier {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(
        mappedBy = "supplier", 
        cascade = CascadeType.ALL, 
        orphanRemoval = true
    )
    private List<BID> bids = new ArrayList<>();

    @CreationTimestamp 
    private Instant createdAt;
}
