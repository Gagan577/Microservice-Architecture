package com.enterprise.product.exception;

/**
 * Exception thrown when a stock reservation operation fails.
 */
public class StockReservationException extends RuntimeException {

    private final String reservationCode;
    private final String reason;

    public StockReservationException(String reservationCode, String reason) {
        super(String.format("Stock reservation failed for code %s: %s", reservationCode, reason));
        this.reservationCode = reservationCode;
        this.reason = reason;
    }

    public StockReservationException(String message) {
        super(message);
        this.reservationCode = null;
        this.reason = message;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public String getReason() {
        return reason;
    }
}
