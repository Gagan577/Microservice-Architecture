package com.enterprise.product.soap;

import jakarta.xml.bind.annotation.*;

/**
 * SOAP request for checking stock availability.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"productId", "quantity"})
@XmlRootElement(name = "StockAvailabilityRequest", namespace = "http://enterprise.com/product/soap")
public class StockAvailabilityRequest {

    @XmlElement(required = true, namespace = "http://enterprise.com/product/soap")
    private String productId;

    @XmlElement(required = true, namespace = "http://enterprise.com/product/soap")
    private Integer quantity;

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
