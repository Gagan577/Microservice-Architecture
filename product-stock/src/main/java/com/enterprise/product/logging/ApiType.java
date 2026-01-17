package com.enterprise.product.logging;

/**
 * Enum representing the type of API being called.
 * Used for categorizing logs and metrics.
 */
public enum ApiType {
    REST,
    SOAP,
    GRAPHQL,
    INTERNAL,
    SCHEDULED,
    ASYNC
}
