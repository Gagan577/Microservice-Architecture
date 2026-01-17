package com.enterprise.product.logging;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Manages correlation IDs for distributed tracing across microservices.
 * Uses MDC (Mapped Diagnostic Context) for thread-safe correlation ID propagation.
 */
public final class CorrelationIdManager {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "traceId";
    public static final String API_TYPE_MDC_KEY = "apiType";
    public static final String SERVICE_NAME_MDC_KEY = "serviceName";

    private static final String SERVICE_NAME = "product-stock";

    private CorrelationIdManager() {
        // Utility class
    }

    /**
     * Generates a new correlation ID.
     */
    public static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Sets the correlation ID in MDC for the current thread.
     */
    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        MDC.put(SERVICE_NAME_MDC_KEY, SERVICE_NAME);
    }

    /**
     * Gets the current correlation ID from MDC.
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }

    /**
     * Sets the API type in MDC.
     */
    public static void setApiType(ApiType apiType) {
        MDC.put(API_TYPE_MDC_KEY, apiType.name());
    }

    /**
     * Gets the current API type from MDC.
     */
    public static String getApiType() {
        return MDC.get(API_TYPE_MDC_KEY);
    }

    /**
     * Clears all MDC values for the current thread.
     */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Removes only correlation-related MDC values.
     */
    public static void clearCorrelationContext() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
        MDC.remove(API_TYPE_MDC_KEY);
        MDC.remove(SERVICE_NAME_MDC_KEY);
    }

    /**
     * Sets up correlation context, generating new ID if not provided.
     */
    public static String setupCorrelation(String existingCorrelationId, ApiType apiType) {
        String correlationId = (existingCorrelationId != null && !existingCorrelationId.isBlank())
                ? existingCorrelationId
                : generateCorrelationId();
        setCorrelationId(correlationId);
        setApiType(apiType);
        return correlationId;
    }
}
