package com.enterprise.shop.logging;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Correlation ID manager for distributed tracing across services.
 * Manages trace IDs and span IDs in the MDC (Mapped Diagnostic Context).
 */
@Component
public class CorrelationIdManager {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";
    public static final String API_TYPE_KEY = "apiType";
    public static final String ENDPOINT_KEY = "endpoint";
    public static final String HTTP_METHOD_KEY = "httpMethod";
    public static final String HEADERS_KEY = "headers";
    public static final String REQUEST_PAYLOAD_KEY = "requestPayload";
    public static final String RESPONSE_PAYLOAD_KEY = "responsePayload";
    public static final String EXECUTION_TIME_KEY = "executionTimeMs";
    public static final String STATUS_CODE_KEY = "statusCode";
    public static final String CLIENT_IP_KEY = "clientIp";
    public static final String USER_AGENT_KEY = "userAgent";
    public static final String ERROR_STACK_KEY = "errorStack";

    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String SPAN_ID_MDC_KEY = "spanId";

    /**
     * Generate a new trace ID.
     */
    public String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate a new span ID.
     */
    public String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * Set trace ID in MDC.
     */
    public void setTraceId(String traceId) {
        MDC.put(TRACE_ID_MDC_KEY, traceId);
    }

    /**
     * Set span ID in MDC.
     */
    public void setSpanId(String spanId) {
        MDC.put(SPAN_ID_MDC_KEY, spanId);
    }

    /**
     * Get current trace ID from MDC.
     */
    public String getTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }

    /**
     * Get current span ID from MDC.
     */
    public String getSpanId() {
        return MDC.get(SPAN_ID_MDC_KEY);
    }

    /**
     * Set or generate trace ID if not present.
     */
    public String ensureTraceId(String existingTraceId) {
        String traceId = existingTraceId;
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
        }
        setTraceId(traceId);
        return traceId;
    }

    /**
     * Set custom MDC attribute.
     */
    public void setMdcAttribute(String key, String value) {
        if (value != null) {
            MDC.put(key, value);
        }
    }

    /**
     * Clear all MDC attributes.
     */
    public void clear() {
        MDC.clear();
    }

    /**
     * Clear specific MDC attribute.
     */
    public void clearAttribute(String key) {
        MDC.remove(key);
    }

    /**
     * Initialize new request context with trace and span IDs.
     */
    public RequestContext initializeRequestContext(String existingTraceId) {
        String traceId = ensureTraceId(existingTraceId);
        String spanId = generateSpanId();
        setSpanId(spanId);
        return new RequestContext(traceId, spanId);
    }

    /**
     * Request context holder.
     */
    public record RequestContext(String traceId, String spanId) {}
}
