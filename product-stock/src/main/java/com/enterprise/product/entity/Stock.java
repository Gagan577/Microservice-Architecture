package com.enterprise.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stock entity representing current inventory levels.
 */
@Entity
@Table(name = "stock", schema = "product_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "warehouse_code", nullable = false, length = 50)
    @Builder.Default
    private String warehouseCode = "MAIN";

    @Column(name = "location_code", length = 50)
    private String locationCode;

    @Column(name = "total_quantity", nullable = false)
    @Builder.Default
    private Integer totalQuantity = 0;

    @Column(name = "available_quantity", nullable = false)
    @Builder.Default
    private Integer availableQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "damaged_quantity", nullable = false)
    @Builder.Default
    private Integer damagedQuantity = 0;

    @Column(name = "in_transit_quantity", nullable = false)
    @Builder.Default
    private Integer inTransitQuantity = 0;

    @Column(name = "last_counted_at")
    private OffsetDateTime lastCountedAt;

    @Column(name = "last_restocked_at")
    private OffsetDateTime lastRestockedAt;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Reserve stock quantity.
     */
    public boolean reserve(int quantity) {
        if (availableQuantity >= quantity) {
            availableQuantity -= quantity;
            reservedQuantity += quantity;
            return true;
        }
        return false;
    }

    /**
     * Release reserved stock.
     */
    public boolean release(int quantity) {
        if (reservedQuantity >= quantity) {
            reservedQuantity -= quantity;
            availableQuantity += quantity;
            return true;
        }
        return false;
    }

    /**
     * Commit reserved stock (convert reservation to sale).
     */
    public boolean commit(int quantity) {
        if (reservedQuantity >= quantity) {
            reservedQuantity -= quantity;
            totalQuantity -= quantity;
            return true;
        }
        return false;
    }

    /**
     * Add stock (receiving inventory).
     */
    public void addStock(int quantity) {
        totalQuantity += quantity;
        availableQuantity += quantity;
        lastRestockedAt = OffsetDateTime.now();
    }

    /**
     * Check if quantity is available for reservation.
     */
    public boolean hasAvailable(int quantity) {
        return availableQuantity >= quantity;
    }
}
