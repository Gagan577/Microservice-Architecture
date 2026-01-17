package com.enterprise.shop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Shop Management Microservice Application
 * 
 * Enterprise-grade microservice for managing shops, orders, and inventory coordination.
 * Communicates with Product-Stock service via REST, SOAP, and GraphQL.
 * 
 * @author Enterprise Architecture Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class ShopManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopManagementApplication.class, args);
    }
}
