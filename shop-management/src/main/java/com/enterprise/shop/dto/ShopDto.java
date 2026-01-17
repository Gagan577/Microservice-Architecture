package com.enterprise.shop.dto;

import com.enterprise.shop.entity.Shop;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO classes for Shop entity operations.
 */
public class ShopDto {

    private ShopDto() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create a new shop")
    public static class CreateRequest {

        @NotBlank(message = "Shop code is required")
        @Size(min = 3, max = 50, message = "Shop code must be between 3 and 50 characters")
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Shop code must contain only uppercase letters, numbers, hyphens, and underscores")
        @Schema(description = "Unique shop identifier code", example = "SHOP-NYC-001")
        private String shopCode;

        @NotBlank(message = "Shop name is required")
        @Size(min = 2, max = 255, message = "Shop name must be between 2 and 255 characters")
        @Schema(description = "Shop display name", example = "Downtown NYC Store")
        private String name;

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Shop description")
        private String description;

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        @Schema(description = "Primary address line", example = "123 Main Street")
        private String addressLine1;

        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        @Schema(description = "Secondary address line", example = "Suite 100")
        private String addressLine2;

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City cannot exceed 100 characters")
        @Schema(description = "City name", example = "New York")
        private String city;

        @Size(max = 100, message = "State cannot exceed 100 characters")
        @Schema(description = "State or province", example = "NY")
        private String state;

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        @Schema(description = "Postal/ZIP code", example = "10001")
        private String postalCode;

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        @Schema(description = "Country name", example = "USA")
        private String country;

        @Pattern(regexp = "^\\+?[0-9\\-\\s()]+$", message = "Invalid phone number format")
        @Size(max = 20, message = "Phone cannot exceed 20 characters")
        @Schema(description = "Contact phone number", example = "+1-212-555-0100")
        private String phone;

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        @Schema(description = "Contact email address", example = "downtown@example.com")
        private String email;

        @Schema(description = "Shop opening hours by day")
        private Map<String, String> openingHours;

        @Schema(description = "Additional metadata")
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to update shop details")
    public static class UpdateRequest {

        @Size(min = 2, max = 255, message = "Shop name must be between 2 and 255 characters")
        @Schema(description = "Shop display name")
        private String name;

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        @Schema(description = "Shop description")
        private String description;

        @Size(max = 255, message = "Address line 1 cannot exceed 255 characters")
        @Schema(description = "Primary address line")
        private String addressLine1;

        @Size(max = 255, message = "Address line 2 cannot exceed 255 characters")
        @Schema(description = "Secondary address line")
        private String addressLine2;

        @Size(max = 100, message = "City cannot exceed 100 characters")
        @Schema(description = "City name")
        private String city;

        @Size(max = 100, message = "State cannot exceed 100 characters")
        @Schema(description = "State or province")
        private String state;

        @Size(max = 20, message = "Postal code cannot exceed 20 characters")
        @Schema(description = "Postal/ZIP code")
        private String postalCode;

        @Size(max = 100, message = "Country cannot exceed 100 characters")
        @Schema(description = "Country name")
        private String country;

        @Pattern(regexp = "^\\+?[0-9\\-\\s()]+$", message = "Invalid phone number format")
        @Size(max = 20, message = "Phone cannot exceed 20 characters")
        @Schema(description = "Contact phone number")
        private String phone;

        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        @Schema(description = "Contact email address")
        private String email;

        @Schema(description = "Shop status")
        private Shop.ShopStatus status;

        @Schema(description = "Shop opening hours by day")
        private Map<String, String> openingHours;

        @Schema(description = "Additional metadata")
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Shop response data")
    public static class Response {

        @Schema(description = "Shop unique identifier")
        private UUID id;

        @Schema(description = "Shop code")
        private String shopCode;

        @Schema(description = "Shop name")
        private String name;

        @Schema(description = "Shop description")
        private String description;

        @Schema(description = "Primary address line")
        private String addressLine1;

        @Schema(description = "Secondary address line")
        private String addressLine2;

        @Schema(description = "City")
        private String city;

        @Schema(description = "State")
        private String state;

        @Schema(description = "Postal code")
        private String postalCode;

        @Schema(description = "Country")
        private String country;

        @Schema(description = "Phone number")
        private String phone;

        @Schema(description = "Email address")
        private String email;

        @Schema(description = "Shop status")
        private Shop.ShopStatus status;

        @Schema(description = "Opening hours")
        private Map<String, String> openingHours;

        @Schema(description = "Additional metadata")
        private Map<String, Object> metadata;

        @Schema(description = "Creation timestamp")
        private OffsetDateTime createdAt;

        @Schema(description = "Last update timestamp")
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Shop summary for list views")
    public static class Summary {

        @Schema(description = "Shop unique identifier")
        private UUID id;

        @Schema(description = "Shop code")
        private String shopCode;

        @Schema(description = "Shop name")
        private String name;

        @Schema(description = "City")
        private String city;

        @Schema(description = "State")
        private String state;

        @Schema(description = "Shop status")
        private Shop.ShopStatus status;
    }
}
