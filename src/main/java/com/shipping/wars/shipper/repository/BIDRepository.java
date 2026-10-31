package com.shipping.wars.shipper.repository;

import com.shipping.wars.shipper.entity.BID;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BIDRepository extends JpaRepository<BID, Long> {

    @Query("""
        SELECT b FROM BID b JOIN FETCH b.supplier
        WHERE b.rfq.referenceId = :refId
        ORDER BY b.totalPrice ASC, b.transitDays ASC, b.submittedAt DESC, b.supplier.id ASC
        """)
    List<BID> findByRfqReferenceIdWithSupplier(@Param("refId") String refId);

    @Query("""
        SELECT b FROM BID b JOIN FETCH b.supplier
        WHERE b.rfq.referenceId = :refId AND b.supplier.id = :supplierId 
        ORDER BY b.submittedAt DESC
    """)
    List<BID> findLatestByRfqAndSupplier(@Param("refId") String refId, @Param("supplierId") Long supplierId);

    @EntityGraph(attributePaths = {"supplier"})
    @Query(value = """
        SELECT DISTINCT ON (b.supplier_id) b.*
        FROM bid b
        WHERE b.rfq_id = :rfqId
        ORDER BY b.submitted_at DESC, b.id DESC
    """, nativeQuery = true)
    List<BID> findLatestBidsByRFQ(@Param("rfqId") Long rfqId);
}
