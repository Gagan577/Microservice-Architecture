package com.enterprise.shop.exception;

/**
 * Exception thrown when external service communication fails.
 */
public class ServiceCommunicationException extends ShopException {

    private final String serviceName;
    private final String endpoint;
    private final int statusCode;

    public ServiceCommunicationException(String serviceName, String endpoint, String message) {
        super(String.format("Failed to communicate with %s at %s: %s", serviceName, endpoint, message), 
              "SERVICE_COMMUNICATION_ERROR");
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.statusCode = 0;
    }

    public ServiceCommunicationException(String serviceName, String endpoint, int statusCode, String message) {
        super(String.format("Failed to communicate with %s at %s (status %d): %s", 
              serviceName, endpoint, statusCode, message), "SERVICE_COMMUNICATION_ERROR");
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.statusCode = statusCode;
    }

    public ServiceCommunicationException(String serviceName, String endpoint, Throwable cause) {
        super(String.format("Failed to communicate with %s at %s: %s", 
              serviceName, endpoint, cause.getMessage()), "SERVICE_COMMUNICATION_ERROR", cause);
        this.serviceName = serviceName;
        this.endpoint = endpoint;
        this.statusCode = 0;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
