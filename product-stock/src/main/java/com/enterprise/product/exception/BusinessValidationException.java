package com.enterprise.product.exception;

/**
 * Exception thrown when business validation fails.
 */
public class BusinessValidationException extends RuntimeException {

    private final String field;
    private final String validationCode;

    public BusinessValidationException(String message) {
        super(message);
        this.field = null;
        this.validationCode = "VALIDATION_ERROR";
    }

    public BusinessValidationException(String field, String message) {
        super(message);
        this.field = field;
        this.validationCode = "FIELD_VALIDATION_ERROR";
    }

    public BusinessValidationException(String field, String message, String validationCode) {
        super(message);
        this.field = field;
        this.validationCode = validationCode;
    }

    public String getField() {
        return field;
    }

    public String getValidationCode() {
        return validationCode;
    }
}
