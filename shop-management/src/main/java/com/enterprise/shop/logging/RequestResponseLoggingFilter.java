package com.enterprise.shop.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP Request/Response logging filter.
 * Captures and logs request headers, body, response body, and execution time.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private final CorrelationIdManager correlationIdManager;
    private final ObjectMapper objectMapper;

    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "x-api-key", "api-key", "password"
    );

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus"
    );

    private static final int MAX_PAYLOAD_LENGTH = 10000;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip logging for excluded paths
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Wrap request and response for body caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Initialize correlation context
            String existingTraceId = request.getHeader(CorrelationIdManager.TRACE_ID_HEADER);
            CorrelationIdManager.RequestContext context = correlationIdManager.initializeRequestContext(existingTraceId);

            // Set trace ID in response header
            response.setHeader(CorrelationIdManager.TRACE_ID_HEADER, context.traceId());
            response.setHeader(CorrelationIdManager.SPAN_ID_HEADER, context.spanId());

            // Determine API type
            ApiType apiType = determineApiType(request);

            // Set MDC attributes
            setMdcAttributes(request, apiType);

            // Log request (before processing)
            logRequest(wrappedRequest, apiType);

            // Process request
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(wrappedRequest, wrappedResponse, executionTime);

            // Copy response body to actual response
            wrappedResponse.copyBodyToResponse();

            // Clear MDC
            correlationIdManager.clear();
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PATHS.stream().anyMatch(path::startsWith);
    }

    private ApiType determineApiType(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contentType = request.getContentType();

        if (path.contains("/ws") || path.contains("/soap") || 
                (contentType != null && contentType.contains("xml"))) {
            return ApiType.SOAP;
        }
        if (path.contains("/graphql")) {
            return ApiType.GRAPHQL;
        }
        return ApiType.REST;
    }

    private void setMdcAttributes(HttpServletRequest request, ApiType apiType) {
        MDC.put(CorrelationIdManager.API_TYPE_KEY, apiType.getValue());
        MDC.put(CorrelationIdManager.ENDPOINT_KEY, request.getRequestURI());
        MDC.put(CorrelationIdManager.HTTP_METHOD_KEY, request.getMethod());
        MDC.put(CorrelationIdManager.CLIENT_IP_KEY, getClientIp(request));
        MDC.put(CorrelationIdManager.USER_AGENT_KEY, request.getHeader("User-Agent"));

        // Log headers (with sensitive data masked)
        Map<String, String> headers = extractHeaders(request);
        try {
            MDC.put(CorrelationIdManager.HEADERS_KEY, objectMapper.writeValueAsString(headers));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize headers", e);
        }
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = SENSITIVE_HEADERS.contains(name.toLowerCase())
                    ? "***MASKED***"
                    : request.getHeader(name);
            headers.put(name, value);
        }
        return headers;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void logRequest(ContentCachingRequestWrapper request, ApiType apiType) {
        log.debug(">>> Incoming {} Request: {} {}", 
                apiType.getValue(), 
                request.getMethod(), 
                request.getRequestURI());
    }

    private void logResponse(ContentCachingRequestWrapper request, 
                             ContentCachingResponseWrapper response, 
                             long executionTime) {

        // Extract request body
        String requestBody = getRequestBody(request);
        if (requestBody != null && !requestBody.isBlank()) {
            MDC.put(CorrelationIdManager.REQUEST_PAYLOAD_KEY, truncatePayload(requestBody));
        }

        // Extract response body
        String responseBody = getResponseBody(response);
        if (responseBody != null && !responseBody.isBlank()) {
            MDC.put(CorrelationIdManager.RESPONSE_PAYLOAD_KEY, truncatePayload(responseBody));
        }

        // Set execution time and status
        MDC.put(CorrelationIdManager.EXECUTION_TIME_KEY, String.valueOf(executionTime));
        MDC.put(CorrelationIdManager.STATUS_CODE_KEY, String.valueOf(response.getStatus()));

        // Log completion
        int status = response.getStatus();
        if (status >= 400) {
            log.warn("<<< Response: {} {} - Status: {} - Time: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    executionTime);
        } else {
            log.info("<<< Response: {} {} - Status: {} - Time: {}ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    executionTime);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] buf = request.getContentAsByteArray();
        if (buf.length > 0) {
            return new String(buf, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            return new String(buf, StandardCharsets.UTF_8);
        }
        return null;
    }

    private String truncatePayload(String payload) {
        if (payload == null) {
            return null;
        }
        if (payload.length() > MAX_PAYLOAD_LENGTH) {
            return payload.substring(0, MAX_PAYLOAD_LENGTH) + "... [TRUNCATED]";
        }
        return payload;
    }
}
