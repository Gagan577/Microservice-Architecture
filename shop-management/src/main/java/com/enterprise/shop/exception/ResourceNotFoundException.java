package com.enterprise.shop.exception;

import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends ShopException {

    private final String resourceType;
    private final String resourceId;

    public ResourceNotFoundException(String resourceType, UUID id) {
        super(String.format("%s not found with ID: %s", resourceType, id), "RESOURCE_NOT_FOUND");
        this.resourceType = resourceType;
        this.resourceId = id.toString();
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found: %s", resourceType, identifier), "RESOURCE_NOT_FOUND");
        this.resourceType = resourceType;
        this.resourceId = identifier;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
