package com.enterprise.shop.service.client;

import com.enterprise.shop.exception.ServiceCommunicationException;
import com.enterprise.shop.logging.ApiType;
import com.enterprise.shop.logging.CorrelationIdManager;
import com.enterprise.shop.service.client.dto.ProductResponse;
import com.enterprise.shop.service.client.dto.StockReservationRequest;
import com.enterprise.shop.service.client.dto.StockReservationResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * HTTP Client for Product-Stock service communication.
 * Supports REST calls with comprehensive logging.
 */
@Slf4j
@Component
public class ProductServiceClient {

    private final WebClient webClient;
    private final CorrelationIdManager correlationIdManager;
    private final int retryAttempts;
    private final long retryDelay;

    public ProductServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${product.service.base-url}") String baseUrl,
            @Value("${product.service.connect-timeout:5000}") int connectTimeout,
            @Value("${product.service.read-timeout:10000}") int readTimeout,
            @Value("${product.service.retry-attempts:3}") int retryAttempts,
            @Value("${product.service.retry-delay:1000}") long retryDelay,
            CorrelationIdManager correlationIdManager) {

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.correlationIdManager = correlationIdManager;
        this.retryAttempts = retryAttempts;
        this.retryDelay = retryDelay;
    }

    /**
     * Reserve stock for an order.
     */
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        String traceId = correlationIdManager.getTraceId();
        String endpoint = "/api/v1/stock/reserve";
        long startTime = System.currentTimeMillis();

        log.info("[{}] [REST] Calling Product Service: POST {} for product: {}", 
                traceId, endpoint, request.getProductCode());

        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.REST.getValue());
        MDC.put(CorrelationIdManager.ENDPOINT_KEY, endpoint);
        MDC.put(CorrelationIdManager.HTTP_METHOD_KEY, "POST");

        try {
            StockReservationResponse response = webClient.post()
                    .uri(endpoint)
                    .header(CorrelationIdManager.TRACE_ID_HEADER, traceId)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> 
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new ServiceCommunicationException(
                                            "Product-Stock", endpoint, 
                                            clientResponse.statusCode().value(), body))))
                    .bodyToMono(StockReservationResponse.class)
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                            .filter(this::isRetryableException)
                            .doBeforeRetry(signal -> log.warn("[{}] Retrying stock reservation, attempt: {}", 
                                    traceId, signal.totalRetries() + 1)))
                    .block(Duration.ofSeconds(30));

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] [REST] Stock reservation completed - Product: {} - Reserved: {} - Time: {}ms",
                    traceId, request.getProductCode(), 
                    response != null ? response.getReservedQuantity() : 0, executionTime);

            return response;

        } catch (WebClientResponseException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] [REST] Stock reservation failed - Status: {} - Time: {}ms", 
                    traceId, e.getStatusCode(), executionTime);
            throw new ServiceCommunicationException("Product-Stock", endpoint, e.getStatusCode().value(), e.getMessage());
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] [REST] Stock reservation error - Time: {}ms - Error: {}", 
                    traceId, executionTime, e.getMessage());
            throw new ServiceCommunicationException("Product-Stock", endpoint, e);
        }
    }

    /**
     * Release reserved stock.
     */
    public void releaseStock(UUID reservationId) {
        String traceId = correlationIdManager.getTraceId();
        String endpoint = "/api/v1/stock/release/" + reservationId;
        long startTime = System.currentTimeMillis();

        log.info("[{}] [REST] Calling Product Service: DELETE {} for reservation: {}", 
                traceId, endpoint, reservationId);

        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.REST.getValue());
        MDC.put(CorrelationIdManager.ENDPOINT_KEY, endpoint);
        MDC.put(CorrelationIdManager.HTTP_METHOD_KEY, "DELETE");

        try {
            webClient.delete()
                    .uri(endpoint)
                    .header(CorrelationIdManager.TRACE_ID_HEADER, traceId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new ServiceCommunicationException(
                                            "Product-Stock", endpoint,
                                            clientResponse.statusCode().value(), body))))
                    .toBodilessEntity()
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                            .filter(this::isRetryableException))
                    .block(Duration.ofSeconds(30));

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] [REST] Stock release completed - Reservation: {} - Time: {}ms",
                    traceId, reservationId, executionTime);

        } catch (WebClientResponseException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] [REST] Stock release failed - Status: {} - Time: {}ms", 
                    traceId, e.getStatusCode(), executionTime);
            throw new ServiceCommunicationException("Product-Stock", endpoint, e.getStatusCode().value(), e.getMessage());
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] [REST] Stock release error - Time: {}ms - Error: {}", 
                    traceId, executionTime, e.getMessage());
            throw new ServiceCommunicationException("Product-Stock", endpoint, e);
        }
    }

    /**
     * Get product details by ID.
     */
    public ProductResponse getProduct(UUID productId) {
        String traceId = correlationIdManager.getTraceId();
        String endpoint = "/api/v1/products/" + productId;
        long startTime = System.currentTimeMillis();

        log.debug("[{}] [REST] Calling Product Service: GET {}", traceId, endpoint);

        try {
            ProductResponse response = webClient.get()
                    .uri(endpoint)
                    .header(CorrelationIdManager.TRACE_ID_HEADER, traceId)
                    .retrieve()
                    .bodyToMono(ProductResponse.class)
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                            .filter(this::isRetryableException))
                    .block(Duration.ofSeconds(30));

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("[{}] [REST] Product fetch completed - Time: {}ms", traceId, executionTime);

            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] [REST] Product fetch error - Time: {}ms - Error: {}", 
                    traceId, executionTime, e.getMessage());
            throw new ServiceCommunicationException("Product-Stock", endpoint, e);
        }
    }

    /**
     * Get multiple products by IDs.
     */
    public List<ProductResponse> getProductsByIds(List<UUID> productIds) {
        String traceId = correlationIdManager.getTraceId();
        String endpoint = "/api/v1/products/batch";
        long startTime = System.currentTimeMillis();

        log.debug("[{}] [REST] Calling Product Service: POST {} with {} products", 
                traceId, endpoint, productIds.size());

        try {
            List<ProductResponse> response = webClient.post()
                    .uri(endpoint)
                    .header(CorrelationIdManager.TRACE_ID_HEADER, traceId)
                    .bodyValue(productIds)
                    .retrieve()
                    .bodyToFlux(ProductResponse.class)
                    .collectList()
                    .retryWhen(Retry.backoff(retryAttempts, Duration.ofMillis(retryDelay))
                            .filter(this::isRetryableException))
                    .block(Duration.ofSeconds(30));

            long executionTime = System.currentTimeMillis() - startTime;
            log.debug("[{}] [REST] Batch product fetch completed - Count: {} - Time: {}ms", 
                    traceId, response != null ? response.size() : 0, executionTime);

            return response;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[{}] [REST] Batch product fetch error - Time: {}ms - Error: {}", 
                    traceId, executionTime, e.getMessage());
            throw new ServiceCommunicationException("Product-Stock", endpoint, e);
        }
    }

    /**
     * Check if exception is retryable.
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status == 503 || status == 504 || status == 429;
        }
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.net.SocketTimeoutException;
    }
}
