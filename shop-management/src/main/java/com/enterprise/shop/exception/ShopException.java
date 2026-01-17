package com.enterprise.shop.exception;

import java.util.UUID;

/**
 * Base exception for Shop Management service.
 */
public abstract class ShopException extends RuntimeException {

    private final String errorCode;
    private final String traceId;

    protected ShopException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = null;
    }

    protected ShopException(String message, String errorCode, String traceId) {
        super(message);
        this.errorCode = errorCode;
        this.traceId = traceId;
    }

    protected ShopException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.traceId = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTraceId() {
        return traceId;
    }
}
