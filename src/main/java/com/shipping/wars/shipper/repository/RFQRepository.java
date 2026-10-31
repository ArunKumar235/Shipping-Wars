package com.shipping.wars.shipper.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.shipping.wars.shipper.entity.RFQ;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository 
public interface RFQRepository extends JpaRepository<RFQ, Long>  {

    @Query("SELECT r FROM RFQ r LEFT JOIN FETCH r.auctionConfig WHERE r.referenceId = :refId")
    Optional<RFQ> findByReferenceId(@Param("refId") String refId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("""
        SELECT r FROM RFQ r
        LEFT JOIN FETCH r.auctionConfig
        WHERE r.referenceId = :refId
        """)
    Optional<RFQ> findForBidSubmission(@Param("refId") String refId);

    @Override 
    @Query("SELECT r FROM RFQ r LEFT JOIN FETCH r.auctionConfig")
    List<RFQ> findAll();

    @Query("SELECT r FROM RFQ r LEFT JOIN FETCH r.auctionConfig WHERE r.bidStartTime <= :now AND :now < r.bidCloseTime")
    List<RFQ> findActiveRFQs(@Param("now") Instant now);
}
