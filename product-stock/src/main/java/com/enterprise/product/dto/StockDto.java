package com.enterprise.product.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO classes for Stock operations.
 */
public class StockDto {

    private StockDto() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update stock quantity")
    public static class UpdateStockRequest {

        @NotNull(message = "Product ID is required")
        @Schema(description = "Product identifier")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Schema(description = "Quantity to add or remove", example = "50")
        private Integer quantity;

        @Size(max = 100, message = "Reference cannot exceed 100 characters")
        @Schema(description = "Reference number (e.g., PO number, shipment ID)", example = "PO-2024-001")
        private String reference;

        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        @Schema(description = "Reason for stock update", example = "New shipment received")
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to reserve stock")
    public static class ReserveStockRequest {

        @NotNull(message = "Product ID is required")
        @Schema(description = "Product identifier")
        private UUID productId;

        @NotNull(message = "Order ID is required")
        @Schema(description = "Order identifier")
        private UUID orderId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Schema(description = "Quantity to reserve", example = "5")
        private Integer quantity;

        @Schema(description = "Reservation expiration in minutes", example = "30")
        private Integer reservationMinutes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Stock reservation response")
    public static class ReservationResponse {

        @Schema(description = "Reservation ID")
        private UUID reservationId;

        @Schema(description = "Reservation code")
        private String reservationCode;

        @Schema(description = "Product ID")
        private UUID productId;

        @Schema(description = "Reserved quantity")
        private Integer quantity;

        @Schema(description = "Reservation status")
        private String status;

        @Schema(description = "Reservation expiration time")
        private LocalDateTime expiresAt;

        @Schema(description = "Confirmation time")
        private LocalDateTime confirmedAt;

        @Schema(description = "Release time")
        private LocalDateTime releasedAt;

        @Schema(description = "Status message")
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Stock availability response")
    public static class AvailabilityResponse {

        @Schema(description = "Product ID")
        private UUID productId;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Requested quantity")
        private Integer requestedQuantity;

        @Schema(description = "Available quantity")
        private Integer availableQuantity;

        @Schema(description = "Whether requested quantity is available")
        private Boolean isAvailable;

        @Schema(description = "Status message")
        private String message;

        @Schema(description = "Timestamp when availability was checked")
        private LocalDateTime checkedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Current stock level response")
    public static class StockLevelResponse {

        @Schema(description = "Stock record ID")
        private UUID id;

        @Schema(description = "Product ID")
        private UUID productId;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Available quantity for sale/reservation")
        private Integer availableQuantity;

        @Schema(description = "Currently reserved quantity")
        private Integer reservedQuantity;

        @Schema(description = "Total quantity (available + reserved)")
        private Integer totalQuantity;

        @Schema(description = "Minimum stock level threshold")
        private Integer minimumStock;

        @Schema(description = "Maximum stock level threshold")
        private Integer maximumStock;

        @Schema(description = "Reorder point threshold")
        private Integer reorderPoint;

        @Schema(description = "Warehouse location")
        private String warehouseLocation;

        @Schema(description = "Whether stock is below minimum threshold")
        private Boolean isLowStock;

        @Schema(description = "Whether stock needs reorder")
        private Boolean needsReorder;

        @Schema(description = "Last update timestamp")
        private LocalDateTime lastUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Stock statistics")
    public static class Statistics {

        @Schema(description = "Total available stock across all products")
        private Long totalAvailableStock;

        @Schema(description = "Total reserved stock across all products")
        private Long totalReservedStock;

        @Schema(description = "Number of items below minimum stock")
        private Long lowStockItemCount;

        @Schema(description = "Number of items out of stock")
        private Long outOfStockItemCount;

        @Schema(description = "Number of pending reservations")
        private Long pendingReservationsCount;
    }
}
