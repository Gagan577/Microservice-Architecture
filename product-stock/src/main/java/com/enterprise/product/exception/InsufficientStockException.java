package com.enterprise.product.exception;

/**
 * Exception thrown when there is insufficient stock for an operation.
 */
public class InsufficientStockException extends RuntimeException {

    private final String productId;
    private final Integer requestedQuantity;
    private final Integer availableQuantity;

    public InsufficientStockException(String productId, Integer requestedQuantity, Integer availableQuantity) {
        super(String.format("Insufficient stock for product %s. Requested: %d, Available: %d",
                productId, requestedQuantity, availableQuantity));
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
}
