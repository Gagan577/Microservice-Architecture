package com.enterprise.shop.dto;

import com.enterprise.shop.entity.Order;
import com.enterprise.shop.entity.OrderItem;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO classes for Order entity operations.
 */
public class OrderDto {

    private OrderDto() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to place a new order")
    public static class PlaceOrderRequest {

        @NotNull(message = "Shop ID is required")
        @Schema(description = "Shop identifier where order is placed", example = "550e8400-e29b-41d4-a716-446655440000")
        private UUID shopId;

        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 255, message = "Customer name must be between 2 and 255 characters")
        @Schema(description = "Customer full name", example = "John Doe")
        private String customerName;

        @NotBlank(message = "Customer email is required")
        @Email(message = "Invalid email format")
        @Schema(description = "Customer email address", example = "john.doe@example.com")
        private String customerEmail;

        @Pattern(regexp = "^\\+?[0-9\\-\\s()]+$", message = "Invalid phone number format")
        @Schema(description = "Customer phone number", example = "+1-555-123-4567")
        private String customerPhone;

        @NotNull(message = "Shipping address is required")
        @Schema(description = "Shipping address details")
        private AddressDto shippingAddress;

        @Schema(description = "Billing address (defaults to shipping if not provided)")
        private AddressDto billingAddress;

        @NotEmpty(message = "At least one order item is required")
        @Valid
        @Schema(description = "Order line items")
        private List<OrderItemRequest> items;

        @Schema(description = "Payment method", example = "CREDIT_CARD")
        private String paymentMethod;

        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        @Schema(description = "Order notes")
        private String notes;

        @Schema(description = "Additional metadata")
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Address details")
    public static class AddressDto {

        @NotBlank(message = "Street is required")
        @Schema(description = "Street address", example = "123 Main Street")
        private String street;

        @Schema(description = "Apartment/Suite number", example = "Apt 4B")
        private String apartment;

        @NotBlank(message = "City is required")
        @Schema(description = "City", example = "New York")
        private String city;

        @NotBlank(message = "State is required")
        @Schema(description = "State/Province", example = "NY")
        private String state;

        @NotBlank(message = "Postal code is required")
        @Schema(description = "Postal/ZIP code", example = "10001")
        private String postalCode;

        @NotBlank(message = "Country is required")
        @Schema(description = "Country", example = "USA")
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Order line item request")
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required")
        @Schema(description = "Product identifier", example = "550e8400-e29b-41d4-a716-446655440001")
        private UUID productId;

        @NotBlank(message = "Product code is required")
        @Schema(description = "Product SKU/code", example = "PROD-001")
        private String productCode;

        @NotBlank(message = "Product name is required")
        @Schema(description = "Product display name", example = "Widget Pro")
        private String productName;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10000, message = "Quantity cannot exceed 10000")
        @Schema(description = "Order quantity", example = "2")
        private Integer quantity;

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
        @Schema(description = "Unit price", example = "29.99")
        private BigDecimal unitPrice;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to cancel an order")
    public static class CancelOrderRequest {

        @NotBlank(message = "Cancellation reason is required")
        @Size(min = 10, max = 1000, message = "Cancellation reason must be between 10 and 1000 characters")
        @Schema(description = "Reason for cancellation", example = "Customer requested cancellation - item no longer needed")
        private String reason;

        @Schema(description = "Whether to release reserved stock", example = "true")
        @Builder.Default
        private boolean releaseStock = true;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Order response data")
    public static class Response {

        @Schema(description = "Order unique identifier")
        private UUID id;

        @Schema(description = "Order number")
        private String orderNumber;

        @Schema(description = "Shop ID")
        private UUID shopId;

        @Schema(description = "Shop code")
        private String shopCode;

        @Schema(description = "Shop name")
        private String shopName;

        @Schema(description = "Customer name")
        private String customerName;

        @Schema(description = "Customer email")
        private String customerEmail;

        @Schema(description = "Customer phone")
        private String customerPhone;

        @Schema(description = "Shipping address")
        private Map<String, String> shippingAddress;

        @Schema(description = "Billing address")
        private Map<String, String> billingAddress;

        @Schema(description = "Order status")
        private Order.OrderStatus status;

        @Schema(description = "Order subtotal")
        private BigDecimal subtotal;

        @Schema(description = "Tax amount")
        private BigDecimal taxAmount;

        @Schema(description = "Shipping amount")
        private BigDecimal shippingAmount;

        @Schema(description = "Discount amount")
        private BigDecimal discountAmount;

        @Schema(description = "Total amount")
        private BigDecimal totalAmount;

        @Schema(description = "Currency")
        private String currency;

        @Schema(description = "Payment method")
        private String paymentMethod;

        @Schema(description = "Payment status")
        private Order.PaymentStatus paymentStatus;

        @Schema(description = "Order notes")
        private String notes;

        @Schema(description = "Order items")
        private List<OrderItemResponse> items;

        @Schema(description = "Order timestamp")
        private OffsetDateTime orderedAt;

        @Schema(description = "Shipped timestamp")
        private OffsetDateTime shippedAt;

        @Schema(description = "Delivered timestamp")
        private OffsetDateTime deliveredAt;

        @Schema(description = "Cancelled timestamp")
        private OffsetDateTime cancelledAt;

        @Schema(description = "Cancellation reason")
        private String cancellationReason;

        @Schema(description = "Creation timestamp")
        private OffsetDateTime createdAt;

        @Schema(description = "Last update timestamp")
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Order item response data")
    public static class OrderItemResponse {

        @Schema(description = "Item unique identifier")
        private UUID id;

        @Schema(description = "Product identifier")
        private UUID productId;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Product name")
        private String productName;

        @Schema(description = "Quantity")
        private Integer quantity;

        @Schema(description = "Unit price")
        private BigDecimal unitPrice;

        @Schema(description = "Discount percentage")
        private BigDecimal discountPercent;

        @Schema(description = "Discount amount")
        private BigDecimal discountAmount;

        @Schema(description = "Tax percentage")
        private BigDecimal taxPercent;

        @Schema(description = "Tax amount")
        private BigDecimal taxAmount;

        @Schema(description = "Line total")
        private BigDecimal lineTotal;

        @Schema(description = "Stock reservation ID")
        private UUID stockReservationId;

        @Schema(description = "Reservation status")
        private OrderItem.ReservationStatus reservationStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Order summary for list views")
    public static class Summary {

        @Schema(description = "Order unique identifier")
        private UUID id;

        @Schema(description = "Order number")
        private String orderNumber;

        @Schema(description = "Shop code")
        private String shopCode;

        @Schema(description = "Customer name")
        private String customerName;

        @Schema(description = "Order status")
        private Order.OrderStatus status;

        @Schema(description = "Total amount")
        private BigDecimal totalAmount;

        @Schema(description = "Item count")
        private int itemCount;

        @Schema(description = "Order timestamp")
        private OffsetDateTime orderedAt;
    }
}
