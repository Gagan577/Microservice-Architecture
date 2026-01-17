package com.enterprise.shop.exception;

/**
 * Exception thrown when duplicate resource creation is attempted.
 */
public class DuplicateResourceException extends ShopException {

    private final String resourceType;
    private final String duplicateField;
    private final String duplicateValue;

    public DuplicateResourceException(String resourceType, String field, String value) {
        super(String.format("%s already exists with %s: %s", resourceType, field, value), "DUPLICATE_RESOURCE");
        this.resourceType = resourceType;
        this.duplicateField = field;
        this.duplicateValue = value;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getDuplicateField() {
        return duplicateField;
    }

    public String getDuplicateValue() {
        return duplicateValue;
    }
}
