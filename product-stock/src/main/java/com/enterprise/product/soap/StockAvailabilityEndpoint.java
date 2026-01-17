package com.enterprise.product.soap;

import com.enterprise.product.dto.StockDto;
import com.enterprise.product.logging.ApiType;
import com.enterprise.product.logging.CorrelationIdManager;
import com.enterprise.product.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * SOAP Endpoint for Stock Availability Check.
 * This endpoint provides SOAP-based access to stock availability verification.
 */
@Endpoint
public class StockAvailabilityEndpoint {

    private static final Logger log = LoggerFactory.getLogger(StockAvailabilityEndpoint.class);
    private static final String NAMESPACE_URI = "http://enterprise.com/product/soap";

    private final StockService stockService;

    public StockAvailabilityEndpoint(StockService stockService) {
        this.stockService = stockService;
    }

    /**
     * SOAP operation to check stock availability.
     * 
     * @param request The SOAP request containing productId and quantity
     * @return SOAP response with availability details
     */
    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "StockAvailabilityRequest")
    @ResponsePayload
    public StockAvailabilityResponse checkStockAvailability(@RequestPayload StockAvailabilityRequest request) {

        // Setup correlation for SOAP request
        String correlationId = CorrelationIdManager.setupCorrelation(null, ApiType.SOAP);

        log.info("SOAP StockAvailability request - productId: {}, quantity: {}, correlationId: {}",
                request.getProductId(), request.getQuantity(), correlationId);

        StockAvailabilityResponse response = new StockAvailabilityResponse();
        response.setCorrelationId(correlationId);

        try {
            UUID productId = UUID.fromString(request.getProductId());
            
            StockDto.AvailabilityResponse availability = 
                    stockService.checkAvailability(productId, request.getQuantity());

            response.setProductId(request.getProductId());
            response.setProductCode(availability.getProductCode());
            response.setRequestedQuantity(request.getQuantity());
            response.setAvailableQuantity(availability.getAvailableQuantity());
            response.setAvailable(availability.getIsAvailable());
            response.setMessage(availability.getMessage());
            response.setCheckedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

            log.info("SOAP StockAvailability response - available: {}, correlationId: {}",
                    availability.getIsAvailable(), correlationId);

        } catch (IllegalArgumentException e) {
            log.error("Invalid productId format: {}", request.getProductId(), e);
            response.setProductId(request.getProductId());
            response.setRequestedQuantity(request.getQuantity());
            response.setAvailable(false);
            response.setMessage("Invalid product ID format");
            response.setCheckedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        } catch (Exception e) {
            log.error("Error checking stock availability: {}", e.getMessage(), e);
            response.setProductId(request.getProductId());
            response.setRequestedQuantity(request.getQuantity());
            response.setAvailable(false);
            response.setMessage("Error: " + e.getMessage());
            response.setCheckedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        } finally {
            CorrelationIdManager.clearCorrelationContext();
        }

        return response;
    }
}
