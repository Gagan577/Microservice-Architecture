package com.enterprise.shop.repository;

import com.enterprise.shop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for OrderItem entity operations.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Find items by order ID.
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Find items by product ID.
     */
    List<OrderItem> findByProductId(UUID productId);

    /**
     * Find items by reservation ID.
     */
    List<OrderItem> findByStockReservationId(UUID reservationId);

    /**
     * Find items with pending reservations.
     */
    List<OrderItem> findByReservationStatus(OrderItem.ReservationStatus status);

    /**
     * Find items by order with reservation status.
     */
    List<OrderItem> findByOrderIdAndReservationStatus(UUID orderId, OrderItem.ReservationStatus status);

    /**
     * Update reservation status for order items.
     */
    @Modifying
    @Query("UPDATE OrderItem oi SET oi.reservationStatus = :status, oi.stockReservationId = :reservationId WHERE oi.id = :itemId")
    int updateReservationStatus(
            @Param("itemId") UUID itemId,
            @Param("status") OrderItem.ReservationStatus status,
            @Param("reservationId") UUID reservationId);

    /**
     * Count total quantity ordered for a product.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.productId = :productId")
    Integer getTotalQuantityOrdered(@Param("productId") UUID productId);

    /**
     * Delete items by order ID.
     */
    void deleteByOrderId(UUID orderId);
}
