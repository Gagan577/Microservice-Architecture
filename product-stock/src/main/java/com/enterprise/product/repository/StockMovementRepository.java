package com.enterprise.product.repository;

import com.enterprise.product.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for StockMovement entity operations (audit trail for stock changes).
 */
@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByProductId(UUID productId, Pageable pageable);

    Page<StockMovement> findByMovementType(StockMovement.MovementType movementType, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.productId = :productId " +
            "AND sm.createdAt BETWEEN :startDate AND :endDate ORDER BY sm.createdAt DESC")
    List<StockMovement> findByProductIdAndDateRange(
            @Param("productId") UUID productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY sm.createdAt DESC")
    Page<StockMovement> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    List<StockMovement> findByReferenceContaining(String reference);

    @Query("SELECT sm.movementType, SUM(sm.quantity) FROM StockMovement sm " +
            "WHERE sm.productId = :productId GROUP BY sm.movementType")
    List<Object[]> getMovementSummaryByProduct(@Param("productId") UUID productId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.productId = :productId " +
            "ORDER BY sm.createdAt DESC")
    List<StockMovement> findRecentMovementsByProduct(@Param("productId") UUID productId, Pageable pageable);

    long countByMovementType(StockMovement.MovementType movementType);

    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE sm.movementType = 'STOCK_IN'")
    Long getTotalStockIn();

    @Query("SELECT SUM(sm.quantity) FROM StockMovement sm WHERE sm.movementType = 'STOCK_OUT'")
    Long getTotalStockOut();

    @Query("SELECT DATE(sm.createdAt), sm.movementType, SUM(sm.quantity) " +
            "FROM StockMovement sm WHERE sm.createdAt >= :since " +
            "GROUP BY DATE(sm.createdAt), sm.movementType ORDER BY DATE(sm.createdAt)")
    List<Object[]> getDailyMovementSummary(@Param("since") LocalDateTime since);
}
