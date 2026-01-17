package com.enterprise.shop.repository;

import com.enterprise.shop.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find order by order number.
     */
    Optional<Order> findByOrderNumber(String orderNumber);

    /**
     * Check if order number exists.
     */
    boolean existsByOrderNumber(String orderNumber);

    /**
     * Find orders by shop ID.
     */
    Page<Order> findByShopId(UUID shopId, Pageable pageable);

    /**
     * Find orders by status.
     */
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    /**
     * Find orders by shop and status.
     */
    Page<Order> findByShopIdAndStatus(UUID shopId, Order.OrderStatus status, Pageable pageable);

    /**
     * Find orders by customer email.
     */
    Page<Order> findByCustomerEmail(String customerEmail, Pageable pageable);

    /**
     * Find orders in date range.
     */
    @Query("SELECT o FROM Order o WHERE o.orderedAt BETWEEN :startDate AND :endDate")
    Page<Order> findOrdersInDateRange(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    /**
     * Find orders by shop and date range.
     */
    @Query("SELECT o FROM Order o WHERE o.shop.id = :shopId AND o.orderedAt BETWEEN :startDate AND :endDate")
    Page<Order> findByShopAndDateRange(
            @Param("shopId") UUID shopId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate,
            Pageable pageable);

    /**
     * Find pending orders older than specified time.
     */
    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.orderedAt < :cutoffTime")
    List<Order> findStalePendingOrders(@Param("cutoffTime") OffsetDateTime cutoffTime);

    /**
     * Count orders by status.
     */
    long countByStatus(Order.OrderStatus status);

    /**
     * Count orders by shop and status.
     */
    long countByShopIdAndStatus(UUID shopId, Order.OrderStatus status);

    /**
     * Calculate total revenue for a shop.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.shop.id = :shopId AND o.status NOT IN ('CANCELLED', 'REFUNDED')")
    BigDecimal calculateShopRevenue(@Param("shopId") UUID shopId);

    /**
     * Calculate total revenue by date range.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderedAt BETWEEN :startDate AND :endDate AND o.status NOT IN ('CANCELLED', 'REFUNDED')")
    BigDecimal calculateRevenueInRange(
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * Get order statistics by status.
     */
    @Query("SELECT o.status, COUNT(o), COALESCE(SUM(o.totalAmount), 0) FROM Order o GROUP BY o.status")
    List<Object[]> getOrderStatsByStatus();

    /**
     * Update order status.
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = :status, o.updatedAt = CURRENT_TIMESTAMP WHERE o.id = :orderId")
    int updateOrderStatus(@Param("orderId") UUID orderId, @Param("status") Order.OrderStatus status);

    /**
     * Find orders with items (fetch join for performance).
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") UUID orderId);

    /**
     * Find orders with items by order number.
     */
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);
}
