package com.enterprise.product.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for Product-Stock service.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI productStockOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Product-Stock Service API")
                        .description("Enterprise Product and Stock Management Microservice. " +
                                "Provides REST, SOAP, and GraphQL endpoints for product catalog " +
                                "and inventory management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Enterprise Architecture Team")
                                .email("architecture@enterprise.com")
                                .url("https://enterprise.com"))
                        .license(new License()
                                .name("Enterprise License")
                                .url("https://enterprise.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("http://product-stock:8082")
                                .description("Container Environment")
                ));
    }
}
