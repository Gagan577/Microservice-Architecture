package com.enterprise.product.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * StockReservation entity for temporary stock holds.
 */
@Entity
@Table(name = "stock_reservations", schema = "product_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "warehouse_code", nullable = false, length = 50)
    private String warehouseCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "order_reference", nullable = false, length = 100)
    private String orderReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "reserved_at", nullable = false)
    private OffsetDateTime reservedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "committed_at")
    private OffsetDateTime committedAt;

    @Column(name = "release_reason", columnDefinition = "TEXT")
    private String releaseReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        reservedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Check if reservation is still valid (not expired and active).
     */
    public boolean isValid() {
        return status == ReservationStatus.ACTIVE && 
               OffsetDateTime.now().isBefore(expiresAt);
    }

    /**
     * Release the reservation.
     */
    public void release(String reason) {
        this.status = ReservationStatus.RELEASED;
        this.releasedAt = OffsetDateTime.now();
        this.releaseReason = reason;
    }

    /**
     * Commit the reservation (convert to actual sale).
     */
    public void commit() {
        this.status = ReservationStatus.COMMITTED;
        this.committedAt = OffsetDateTime.now();
    }

    /**
     * Mark as expired.
     */
    public void expire() {
        this.status = ReservationStatus.EXPIRED;
        this.releaseReason = "Reservation expired";
    }

    public enum ReservationStatus {
        ACTIVE,
        RELEASED,
        COMMITTED,
        EXPIRED
    }
}
