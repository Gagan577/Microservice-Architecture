package com.enterprise.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Product Stock Microservice Application
 * 
 * Enterprise-grade microservice for managing products and stock inventory.
 * Provides REST, SOAP, and GraphQL APIs for stock operations.
 * 
 * @author Enterprise Architecture Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ProductStockApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductStockApplication.class, args);
    }
}
