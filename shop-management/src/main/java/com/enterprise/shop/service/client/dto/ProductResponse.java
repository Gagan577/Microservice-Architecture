package com.enterprise.shop.service.client.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response DTO for product details from Product Stock service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String productCode;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private Integer totalQuantity;
    private String status;
}
