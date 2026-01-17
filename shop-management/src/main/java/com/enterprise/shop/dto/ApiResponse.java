package com.enterprise.shop.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Common API response wrapper classes.
 */
public class ApiResponse {

    private ApiResponse() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Standard API response wrapper")
    public static class Response<T> {

        @Schema(description = "Response status", example = "success")
        private String status;

        @Schema(description = "Response message")
        private String message;

        @Schema(description = "Response data payload")
        private T data;

        @Schema(description = "Trace ID for request tracking")
        private String traceId;

        @Schema(description = "Response timestamp")
        private OffsetDateTime timestamp;

        public static <T> Response<T> success(T data) {
            return Response.<T>builder()
                    .status("success")
                    .data(data)
                    .timestamp(OffsetDateTime.now())
                    .build();
        }

        public static <T> Response<T> success(T data, String message) {
            return Response.<T>builder()
                    .status("success")
                    .message(message)
                    .data(data)
                    .timestamp(OffsetDateTime.now())
                    .build();
        }

        public static <T> Response<T> error(String message) {
            return Response.<T>builder()
                    .status("error")
                    .message(message)
                    .timestamp(OffsetDateTime.now())
                    .build();
        }

        public static <T> Response<T> error(String message, String traceId) {
            return Response.<T>builder()
                    .status("error")
                    .message(message)
                    .traceId(traceId)
                    .timestamp(OffsetDateTime.now())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Paginated response wrapper")
    public static class PagedResponse<T> {

        @Schema(description = "Response status")
        private String status;

        @Schema(description = "Page data items")
        private List<T> data;

        @Schema(description = "Pagination metadata")
        private PageMetadata pagination;

        @Schema(description = "Trace ID")
        private String traceId;

        @Schema(description = "Response timestamp")
        private OffsetDateTime timestamp;

        public static <T> PagedResponse<T> of(List<T> data, int page, int size, long totalElements, int totalPages) {
            return PagedResponse.<T>builder()
                    .status("success")
                    .data(data)
                    .pagination(PageMetadata.builder()
                            .page(page)
                            .size(size)
                            .totalElements(totalElements)
                            .totalPages(totalPages)
                            .build())
                    .timestamp(OffsetDateTime.now())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Pagination metadata")
    public static class PageMetadata {

        @Schema(description = "Current page number (0-based)", example = "0")
        private int page;

        @Schema(description = "Page size", example = "20")
        private int size;

        @Schema(description = "Total number of elements", example = "100")
        private long totalElements;

        @Schema(description = "Total number of pages", example = "5")
        private int totalPages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error response with details")
    public static class ErrorResponse {

        @Schema(description = "Error status", example = "error")
        private String status;

        @Schema(description = "Error code", example = "VALIDATION_ERROR")
        private String code;

        @Schema(description = "Error message")
        private String message;

        @Schema(description = "Detailed error information")
        private List<FieldError> errors;

        @Schema(description = "Request path", example = "/api/v1/shops")
        private String path;

        @Schema(description = "Trace ID for support")
        private String traceId;

        @Schema(description = "Error timestamp")
        private OffsetDateTime timestamp;

        public static ErrorResponse of(String code, String message, String path, String traceId) {
            return ErrorResponse.builder()
                    .status("error")
                    .code(code)
                    .message(message)
                    .path(path)
                    .traceId(traceId)
                    .timestamp(OffsetDateTime.now())
                    .build();
        }

        public static ErrorResponse withErrors(String code, String message, List<FieldError> errors, String path, String traceId) {
            return ErrorResponse.builder()
                    .status("error")
                    .code(code)
                    .message(message)
                    .errors(errors)
                    .path(path)
                    .traceId(traceId)
                    .timestamp(OffsetDateTime.now())
                    .build();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Field-level validation error")
    public static class FieldError {

        @Schema(description = "Field name", example = "email")
        private String field;

        @Schema(description = "Rejected value")
        private Object rejectedValue;

        @Schema(description = "Error message", example = "Invalid email format")
        private String message;
    }
}
