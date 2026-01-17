package com.enterprise.product.dto;

import com.enterprise.product.entity.Product;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO classes for Product entity operations.
 */
public class ProductDto {

    private ProductDto() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to add a new product")
    public static class CreateRequest {

        @NotBlank(message = "Product code is required")
        @Size(min = 3, max = 50, message = "Product code must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Product code must contain only uppercase letters, numbers, hyphens, and underscores")
        @Schema(description = "Unique product identifier code", example = "PROD-001")
        private String productCode;

        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        @Schema(description = "Product display name", example = "Widget Pro")
        private String name;

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Product description")
        private String description;

        @Size(max = 100, message = "Category cannot exceed 100 characters")
        @Schema(description = "Product category", example = "Electronics")
        private String category;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Schema(description = "Unit selling price", example = "29.99")
        private BigDecimal price;

        @DecimalMin(value = "0.00", message = "Weight cannot be negative")
        @Schema(description = "Product weight in kg", example = "1.5")
        private BigDecimal weight;

        @Size(max = 100, message = "Dimensions cannot exceed 100 characters")
        @Schema(description = "Product dimensions (LxWxH)", example = "10x5x3 cm")
        private String dimensions;

        @Schema(description = "Initial stock quantity", example = "100")
        @Min(value = 0, message = "Initial stock cannot be negative")
        private Integer initialStock;

        @Schema(description = "Minimum stock level alert", example = "10")
        private Integer minimumStock;

        @Schema(description = "Maximum stock level", example = "1000")
        private Integer maximumStock;

        @Schema(description = "Reorder point", example = "20")
        private Integer reorderPoint;

        @Schema(description = "Warehouse location", example = "A-1-2")
        private String warehouseLocation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update product details")
    public static class UpdateRequest {

        @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
        @Schema(description = "Product display name")
        private String name;

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Product description")
        private String description;

        @Size(max = 100, message = "Category cannot exceed 100 characters")
        @Schema(description = "Product category")
        private String category;

        @DecimalMin(value = "0.00", message = "Price cannot be negative")
        @Schema(description = "Unit selling price")
        private BigDecimal price;

        @DecimalMin(value = "0.00", message = "Weight cannot be negative")
        @Schema(description = "Product weight in kg")
        private BigDecimal weight;

        @Size(max = 100, message = "Dimensions cannot exceed 100 characters")
        @Schema(description = "Product dimensions")
        private String dimensions;

        @Schema(description = "Product status")
        private Product.ProductStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Product response data")
    public static class Response {

        @Schema(description = "Product unique identifier")
        private UUID id;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Product name")
        private String name;

        @Schema(description = "Product description")
        private String description;

        @Schema(description = "Product category")
        private String category;

        @Schema(description = "Unit price")
        private BigDecimal price;

        @Schema(description = "Product weight in kg")
        private BigDecimal weight;

        @Schema(description = "Product dimensions")
        private String dimensions;

        @Schema(description = "Product status")
        private Product.ProductStatus status;

        @Schema(description = "Creation timestamp")
        private LocalDateTime createdAt;

        @Schema(description = "Last update timestamp")
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Product summary for list views")
    public static class Summary {

        @Schema(description = "Product unique identifier")
        private UUID id;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Product name")
        private String name;

        @Schema(description = "Category")
        private String category;

        @Schema(description = "Unit price")
        private BigDecimal price;

        @Schema(description = "Available quantity")
        private Integer availableQuantity;

        @Schema(description = "Product status")
        private Product.ProductStatus status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Product statistics")
    public static class Statistics {

        @Schema(description = "Total number of products")
        private Long totalProducts;

        @Schema(description = "Number of active products")
        private Long activeProducts;

        @Schema(description = "Number of inactive products")
        private Long inactiveProducts;

        @Schema(description = "Number of discontinued products")
        private Long discontinuedProducts;

        @Schema(description = "Total number of categories")
        private Integer totalCategories;

        @Schema(description = "List of all categories")
        private java.util.List<String> categories;
    }
}
