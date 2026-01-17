package com.enterprise.product.exception;

import com.enterprise.product.dto.ApiResponse;
import com.enterprise.product.logging.CorrelationIdManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST APIs.
 * Provides consistent error responses with correlation IDs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {} - correlationId: {}",
                ex.getMessage(), CorrelationIdManager.getCorrelationId());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("RESOURCE_NOT_FOUND")
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(
            DuplicateResourceException ex, WebRequest request) {

        log.warn("Duplicate resource: {} - correlationId: {}",
                ex.getMessage(), CorrelationIdManager.getCorrelationId());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("DUPLICATE_RESOURCE")
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInsufficientStockException(
            InsufficientStockException ex, WebRequest request) {

        log.warn("Insufficient stock: {} - correlationId: {}",
                ex.getMessage(), CorrelationIdManager.getCorrelationId());

        Map<String, Object> details = new HashMap<>();
        details.put("productId", ex.getProductId());
        details.put("requestedQuantity", ex.getRequestedQuantity());
        details.put("availableQuantity", ex.getAvailableQuantity());

        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("INSUFFICIENT_STOCK")
                .data(details)
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(StockReservationException.class)
    public ResponseEntity<ApiResponse<Void>> handleStockReservationException(
            StockReservationException ex, WebRequest request) {

        log.error("Stock reservation failed: {} - correlationId: {}",
                ex.getMessage(), CorrelationIdManager.getCorrelationId());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("STOCK_RESERVATION_ERROR")
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessValidationException(
            BusinessValidationException ex, WebRequest request) {

        log.warn("Business validation failed: {} - correlationId: {}",
                ex.getMessage(), CorrelationIdManager.getCorrelationId());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode(ex.getValidationCode())
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {} - correlationId: {}",
                errors, CorrelationIdManager.getCorrelationId());

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .data(errors)
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {} - correlationId: {}",
                ex.getMessage(), CorrelationIdManager.getCorrelationId());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message(ex.getMessage())
                .errorCode("INVALID_ARGUMENT")
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtException(
            Exception ex, WebRequest request) {

        log.error("Unhandled exception - correlationId: {}", CorrelationIdManager.getCorrelationId(), ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("An unexpected error occurred. Please contact support.")
                .errorCode("INTERNAL_SERVER_ERROR")
                .traceId(CorrelationIdManager.getCorrelationId())
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
