package com.enterprise.shop.logging;

/**
 * Enumeration of API types for logging categorization.
 */
public enum ApiType {
    REST("REST"),
    SOAP("SOAP"),
    GRAPHQL("GRAPHQL"),
    INTERNAL("INTERNAL"),
    SCHEDULED("SCHEDULED"),
    WEBHOOK("WEBHOOK");

    private final String value;

    ApiType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
