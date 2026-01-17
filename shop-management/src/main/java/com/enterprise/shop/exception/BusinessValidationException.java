package com.enterprise.shop.exception;

/**
 * Exception thrown when a business rule is violated.
 */
public class BusinessValidationException extends ShopException {

    public BusinessValidationException(String message) {
        super(message, "BUSINESS_VALIDATION_ERROR");
    }

    public BusinessValidationException(String message, String errorCode) {
        super(message, errorCode);
    }
}
