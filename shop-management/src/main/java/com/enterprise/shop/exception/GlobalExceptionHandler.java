package com.enterprise.shop.exception;

import com.enterprise.shop.dto.ApiResponse;
import com.enterprise.shop.logging.CorrelationIdManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * Provides consistent error responses across all endpoints.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final CorrelationIdManager correlationIdManager;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.warn("[{}] Resource not found: {} - {}", traceId, ex.getResourceType(), ex.getResourceId());
        
        MDC.put(CorrelationIdManager.ERROR_STACK_KEY, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.ErrorResponse.of(
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.warn("[{}] Duplicate resource: {}", traceId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.ErrorResponse.of(
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleBusinessValidation(
            BusinessValidationException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.warn("[{}] Business validation failed: {}", traceId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.ErrorResponse.of(
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(StockReservationException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleStockReservation(
            StockReservationException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.warn("[{}] Stock reservation failed: {}", traceId, ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.ErrorResponse.of(
                        ex.getErrorCode(),
                        ex.getMessage(),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(ServiceCommunicationException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleServiceCommunication(
            ServiceCommunicationException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.error("[{}] Service communication failed: {} -> {}", 
                traceId, ex.getServiceName(), ex.getMessage());
        
        MDC.put(CorrelationIdManager.ERROR_STACK_KEY, getStackTrace(ex));
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.ErrorResponse.of(
                        ex.getErrorCode(),
                        "External service temporarily unavailable. Please try again later.",
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        List<ApiResponse.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> ApiResponse.FieldError.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("[{}] Validation failed with {} errors", traceId, fieldErrors.size());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.ErrorResponse.withErrors(
                        "VALIDATION_ERROR",
                        "Request validation failed",
                        fieldErrors,
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        List<ApiResponse.FieldError> fieldErrors = ex.getConstraintViolations()
                .stream()
                .map(violation -> ApiResponse.FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .rejectedValue(violation.getInvalidValue())
                        .message(violation.getMessage())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.ErrorResponse.withErrors(
                        "VALIDATION_ERROR",
                        "Constraint validation failed",
                        fieldErrors,
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.error("[{}] Data integrity violation: {}", traceId, ex.getMostSpecificCause().getMessage());
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.ErrorResponse.of(
                        "DATA_INTEGRITY_ERROR",
                        "Data integrity constraint violated",
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.warn("[{}] Malformed request body", traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.ErrorResponse.of(
                        "MALFORMED_REQUEST",
                        "Request body is malformed or missing",
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.ErrorResponse.of(
                        "METHOD_NOT_ALLOWED",
                        String.format("Method '%s' is not supported for this endpoint", ex.getMethod()),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.ErrorResponse.of(
                        "UNSUPPORTED_MEDIA_TYPE",
                        String.format("Media type '%s' is not supported", ex.getContentType()),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.ErrorResponse.of(
                        "MISSING_PARAMETER",
                        String.format("Required parameter '%s' is missing", ex.getParameterName()),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.ErrorResponse.of(
                        "TYPE_MISMATCH",
                        String.format("Parameter '%s' has invalid type", ex.getName()),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleNoHandlerFound(
            NoHandlerFoundException ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.ErrorResponse.of(
                        "ENDPOINT_NOT_FOUND",
                        String.format("Endpoint '%s %s' not found", ex.getHttpMethod(), ex.getRequestURL()),
                        request.getRequestURI(),
                        traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse.ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String traceId = correlationIdManager.getTraceId();
        log.error("[{}] Unexpected error: {}", traceId, ex.getMessage(), ex);
        
        MDC.put(CorrelationIdManager.ERROR_STACK_KEY, getStackTrace(ex));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.ErrorResponse.of(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred. Please contact support with trace ID: " + traceId,
                        request.getRequestURI(),
                        traceId));
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage());
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\n\tat ").append(element);
        }
        return sb.toString();
    }
}
