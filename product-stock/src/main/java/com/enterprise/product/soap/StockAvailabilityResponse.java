package com.enterprise.product.soap;

import jakarta.xml.bind.annotation.*;
import java.time.LocalDateTime;

/**
 * SOAP response for stock availability check.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "productId",
        "productCode",
        "requestedQuantity",
        "availableQuantity",
        "available",
        "message",
        "checkedAt",
        "correlationId"
})
@XmlRootElement(name = "StockAvailabilityResponse", namespace = "http://enterprise.com/product/soap")
public class StockAvailabilityResponse {

    @XmlElement(required = true, namespace = "http://enterprise.com/product/soap")
    private String productId;

    @XmlElement(namespace = "http://enterprise.com/product/soap")
    private String productCode;

    @XmlElement(required = true, namespace = "http://enterprise.com/product/soap")
    private Integer requestedQuantity;

    @XmlElement(required = true, namespace = "http://enterprise.com/product/soap")
    private Integer availableQuantity;

    @XmlElement(required = true, namespace = "http://enterprise.com/product/soap")
    private Boolean available;

    @XmlElement(namespace = "http://enterprise.com/product/soap")
    private String message;

    @XmlElement(namespace = "http://enterprise.com/product/soap")
    private String checkedAt;

    @XmlElement(namespace = "http://enterprise.com/product/soap")
    private String correlationId;

    // Getters and Setters
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(String checkedAt) {
        this.checkedAt = checkedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}
