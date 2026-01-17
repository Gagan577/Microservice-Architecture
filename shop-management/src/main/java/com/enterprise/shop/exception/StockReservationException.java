package com.enterprise.shop.exception;

/**
 * Exception thrown when stock reservation fails.
 */
public class StockReservationException extends ShopException {

    private final String productCode;
    private final int requestedQuantity;
    private final int availableQuantity;

    public StockReservationException(String productCode, int requested, int available) {
        super(String.format("Insufficient stock for product %s. Requested: %d, Available: %d", 
              productCode, requested, available), "STOCK_RESERVATION_FAILED");
        this.productCode = productCode;
        this.requestedQuantity = requested;
        this.availableQuantity = available;
    }

    public StockReservationException(String message) {
        super(message, "STOCK_RESERVATION_FAILED");
        this.productCode = null;
        this.requestedQuantity = 0;
        this.availableQuantity = 0;
    }

    public String getProductCode() {
        return productCode;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
