package com.enterprise.product.repository;

import com.enterprise.product.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Stock entity operations with pessimistic locking support.
 */
@Repository
public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductId(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId")
    Optional<Stock> findByProductIdWithLock(@Param("productId") UUID productId);

    List<Stock> findByProductIdIn(List<UUID> productIds);

    @Query("SELECT s FROM Stock s WHERE s.availableQuantity < s.minimumStock")
    List<Stock> findLowStockItems();

    @Query("SELECT s FROM Stock s WHERE s.availableQuantity = 0")
    List<Stock> findOutOfStockItems();

    @Query("SELECT s FROM Stock s WHERE s.reservedQuantity > 0")
    List<Stock> findItemsWithReservations();

    @Modifying
    @Query("UPDATE Stock s SET s.availableQuantity = s.availableQuantity + :quantity, " +
            "s.version = s.version + 1, s.lastUpdated = CURRENT_TIMESTAMP WHERE s.productId = :productId")
    int addStock(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Stock s SET s.availableQuantity = s.availableQuantity - :quantity, " +
            "s.reservedQuantity = s.reservedQuantity + :quantity, " +
            "s.version = s.version + 1, s.lastUpdated = CURRENT_TIMESTAMP " +
            "WHERE s.productId = :productId AND s.availableQuantity >= :quantity")
    int reserveStock(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Stock s SET s.reservedQuantity = s.reservedQuantity - :quantity, " +
            "s.version = s.version + 1, s.lastUpdated = CURRENT_TIMESTAMP " +
            "WHERE s.productId = :productId AND s.reservedQuantity >= :quantity")
    int releaseReservation(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    @Modifying
    @Query("UPDATE Stock s SET s.reservedQuantity = s.reservedQuantity - :quantity, " +
            "s.version = s.version + 1, s.lastUpdated = CURRENT_TIMESTAMP " +
            "WHERE s.productId = :productId AND s.reservedQuantity >= :quantity")
    int confirmReservation(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    @Query("SELECT CASE WHEN s.availableQuantity >= :quantity THEN true ELSE false END " +
            "FROM Stock s WHERE s.productId = :productId")
    boolean isStockAvailable(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    @Query("SELECT SUM(s.availableQuantity) FROM Stock s")
    Long getTotalAvailableStock();

    @Query("SELECT SUM(s.reservedQuantity) FROM Stock s")
    Long getTotalReservedStock();
}
