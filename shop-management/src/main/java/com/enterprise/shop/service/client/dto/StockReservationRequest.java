package com.enterprise.shop.service.client.dto;

import lombok.*;

import java.util.UUID;

/**
 * Request DTO for stock reservation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReservationRequest {
    private UUID productId;
    private String productCode;
    private Integer quantity;
    private String orderReference;
    private Integer expirationMinutes;
}
