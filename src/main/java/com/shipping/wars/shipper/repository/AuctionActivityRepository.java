package com.shipping.wars.shipper.repository;

import com.shipping.wars.shipper.entity.AuctionActivityLog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionActivityRepository extends JpaRepository<AuctionActivityLog, Long> {

    @Query("""
            SELECT a FROM AuctionActivityLog a
            LEFT JOIN FETCH a.bid b
            LEFT JOIN FETCH b.supplier
            WHERE a.rfq.referenceId = :refId
            ORDER BY a.createdAt DESC
    """)
    List<AuctionActivityLog> findByRfqReferenceIdWithBid(@Param("refId") String refId);
}
