package com.enterprise.product.repository;

import com.enterprise.product.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for StockReservation entity operations.
 */
@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    Optional<StockReservation> findByReservationCode(String reservationCode);

    List<StockReservation> findByOrderId(UUID orderId);

    List<StockReservation> findByProductIdAndStatus(UUID productId, StockReservation.ReservationStatus status);

    List<StockReservation> findByStatus(StockReservation.ReservationStatus status);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.status = 'PENDING' AND sr.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("now") LocalDateTime now);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.productId = :productId AND sr.status = 'PENDING'")
    List<StockReservation> findPendingReservationsByProduct(@Param("productId") UUID productId);

    @Query("SELECT SUM(sr.quantity) FROM StockReservation sr " +
            "WHERE sr.productId = :productId AND sr.status = 'PENDING'")
    Integer getTotalPendingReservations(@Param("productId") UUID productId);

    @Modifying
    @Query("UPDATE StockReservation sr SET sr.status = 'EXPIRED', sr.releasedAt = CURRENT_TIMESTAMP " +
            "WHERE sr.status = 'PENDING' AND sr.expiresAt < :now")
    int expireOldReservations(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE StockReservation sr SET sr.status = :newStatus, sr.confirmedAt = CURRENT_TIMESTAMP " +
            "WHERE sr.reservationCode = :reservationCode AND sr.status = 'PENDING'")
    int confirmReservation(@Param("reservationCode") String reservationCode,
                           @Param("newStatus") StockReservation.ReservationStatus newStatus);

    @Modifying
    @Query("UPDATE StockReservation sr SET sr.status = 'RELEASED', sr.releasedAt = CURRENT_TIMESTAMP " +
            "WHERE sr.reservationCode = :reservationCode AND sr.status = 'PENDING'")
    int releaseReservation(@Param("reservationCode") String reservationCode);

    long countByStatus(StockReservation.ReservationStatus status);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.orderId = :orderId AND sr.status = 'PENDING'")
    List<StockReservation> findPendingReservationsByOrder(@Param("orderId") UUID orderId);

    boolean existsByReservationCode(String reservationCode);
}
