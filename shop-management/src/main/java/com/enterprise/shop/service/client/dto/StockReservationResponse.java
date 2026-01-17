package com.enterprise.shop.service.client.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for stock reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationResponse {
    private UUID reservationId;
    private UUID productId;
    private String productCode;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private boolean success;
    private String message;
    private OffsetDateTime expiresAt;
}
