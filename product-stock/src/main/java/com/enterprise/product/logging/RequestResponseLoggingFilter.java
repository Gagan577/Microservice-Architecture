package com.enterprise.product.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter that logs all HTTP requests and responses with correlation IDs.
 * Supports REST, SOAP, and GraphQL requests.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_PAYLOAD_LENGTH = 10000;

    private final ObjectMapper objectMapper;

    public RequestResponseLoggingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        // Setup correlation context
        String correlationId = request.getHeader(CorrelationIdManager.CORRELATION_ID_HEADER);
        ApiType apiType = determineApiType(request);
        correlationId = CorrelationIdManager.setupCorrelation(correlationId, apiType);

        // Add correlation ID to response header
        response.setHeader(CorrelationIdManager.CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();

        try {
            // Log request
            logRequest(wrappedRequest, correlationId, apiType);

            // Process request
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(wrappedRequest, wrappedResponse, correlationId, apiType, duration);

            // Copy content to response
            wrappedResponse.copyBodyToResponse();

            // Clear MDC
            CorrelationIdManager.clearCorrelationContext();
        }
    }

    private ApiType determineApiType(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contentType = request.getContentType();

        if (uri.contains("/graphql")) {
            return ApiType.GRAPHQL;
        } else if (uri.contains("/ws/") || uri.contains("/soap/") ||
                (contentType != null && contentType.contains("xml"))) {
            return ApiType.SOAP;
        }
        return ApiType.REST;
    }

    private void logRequest(ContentCachingRequestWrapper request, String correlationId, ApiType apiType) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "HTTP_REQUEST");
            logData.put("correlationId", correlationId);
            logData.put("apiType", apiType.name());
            logData.put("method", request.getMethod());
            logData.put("uri", request.getRequestURI());
            logData.put("queryString", request.getQueryString());
            logData.put("remoteAddr", request.getRemoteAddr());
            logData.put("userAgent", request.getHeader("User-Agent"));
            logData.put("headers", getHeadersMap(request));

            // For POST/PUT, log body (after reading)
            // Note: Body will be logged in response to avoid reading issues

            log.info("Incoming request: {}", objectMapper.writeValueAsString(logData));
        } catch (Exception e) {
            log.warn("Failed to log request", e);
        }
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
                             String correlationId, ApiType apiType, long duration) {
        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("event", "HTTP_RESPONSE");
            logData.put("correlationId", correlationId);
            logData.put("apiType", apiType.name());
            logData.put("method", request.getMethod());
            logData.put("uri", request.getRequestURI());
            logData.put("status", response.getStatus());
            logData.put("durationMs", duration);

            // Log request body
            String requestBody = getRequestBody(request);
            if (requestBody != null && !requestBody.isEmpty()) {
                logData.put("requestBody", truncatePayload(requestBody));
            }

            // Log response body
            String responseBody = getResponseBody(response);
            if (responseBody != null && !responseBody.isEmpty()) {
                logData.put("responseBody", truncatePayload(responseBody));
            }

            if (response.getStatus() >= 400) {
                log.error("Request completed with error: {}", objectMapper.writeValueAsString(logData));
            } else {
                log.info("Request completed: {}", objectMapper.writeValueAsString(logData));
            }
        } catch (Exception e) {
            log.warn("Failed to log response", e);
        }
    }

    private Map<String, String> getHeadersMap(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            // Don't log sensitive headers
            if (!name.equalsIgnoreCase("authorization") && !name.equalsIgnoreCase("cookie")) {
                headers.put(name, request.getHeader(name));
            }
        });
        return headers;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String truncatePayload(String payload) {
        if (payload.length() > MAX_PAYLOAD_LENGTH) {
            return payload.substring(0, MAX_PAYLOAD_LENGTH) + "... [TRUNCATED]";
        }
        return payload;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.contains("/actuator") || uri.contains("/health") || uri.contains("/favicon");
    }
}
