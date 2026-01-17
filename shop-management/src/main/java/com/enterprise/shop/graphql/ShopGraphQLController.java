package com.enterprise.shop.graphql;

import com.enterprise.shop.dto.ShopDto;
import com.enterprise.shop.entity.Order;
import com.enterprise.shop.entity.Shop;
import com.enterprise.shop.logging.ApiType;
import com.enterprise.shop.logging.CorrelationIdManager;
import com.enterprise.shop.repository.OrderRepository;
import com.enterprise.shop.repository.ShopRepository;
import com.enterprise.shop.service.client.ProductServiceClient;
import com.enterprise.shop.service.client.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GraphQL Controller for Shop Management queries.
 * Implements cross-service queries to Product-Stock service.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ShopGraphQLController {

    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final CorrelationIdManager correlationIdManager;

    @QueryMapping
    public Shop shop(@Argument String id) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: shop(id: {})", traceId, id);
        long startTime = System.currentTimeMillis();

        try {
            Shop shop = shopRepository.findById(UUID.fromString(id)).orElse(null);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] [GRAPHQL] Query completed - shop - Time: {}ms", traceId, executionTime);
            
            return shop;
        } catch (Exception e) {
            log.error("[{}] [GRAPHQL] Query failed - shop - Error: {}", traceId, e.getMessage());
            throw e;
        }
    }

    @QueryMapping
    public Shop shopByCode(@Argument String code) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: shopByCode(code: {})", traceId, code);
        long startTime = System.currentTimeMillis();

        try {
            Shop shop = shopRepository.findByShopCode(code).orElse(null);
            
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] [GRAPHQL] Query completed - shopByCode - Time: {}ms", traceId, executionTime);
            
            return shop;
        } catch (Exception e) {
            log.error("[{}] [GRAPHQL] Query failed - shopByCode - Error: {}", traceId, e.getMessage());
            throw e;
        }
    }

    @QueryMapping
    public ShopConnection shops(@Argument Integer page, @Argument Integer size, @Argument String status) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: shops(page: {}, size: {}, status: {})", traceId, page, size, status);
        long startTime = System.currentTimeMillis();

        try {
            PageRequest pageRequest = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
            Page<Shop> shopPage;

            if (status != null) {
                shopPage = shopRepository.findByStatus(Shop.ShopStatus.valueOf(status), pageRequest);
            } else {
                shopPage = shopRepository.findAll(pageRequest);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] [GRAPHQL] Query completed - shops - Count: {} - Time: {}ms", 
                    traceId, shopPage.getTotalElements(), executionTime);

            return new ShopConnection(
                    shopPage.getContent(),
                    new PageInfo(
                            shopPage.getNumber(),
                            shopPage.getSize(),
                            shopPage.getTotalElements(),
                            shopPage.getTotalPages(),
                            shopPage.hasNext(),
                            shopPage.hasPrevious()
                    )
            );
        } catch (Exception e) {
            log.error("[{}] [GRAPHQL] Query failed - shops - Error: {}", traceId, e.getMessage());
            throw e;
        }
    }

    /**
     * Cross-service GraphQL query that fetches shop with live stock from Product-Stock service.
     */
    @QueryMapping
    public ShopWithStock shopWithStock(@Argument String shopId) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: shopWithStock(shopId: {}) - Cross-service call to Product-Stock", 
                traceId, shopId);
        long startTime = System.currentTimeMillis();

        try {
            // Fetch shop
            Shop shop = shopRepository.findById(UUID.fromString(shopId))
                    .orElseThrow(() -> new RuntimeException("Shop not found: " + shopId));

            // Fetch live stock from Product-Stock service via REST
            log.debug("[{}] [GRAPHQL] Fetching live stock data from Product-Stock service", traceId);
            List<StockItem> stockItems = fetchLiveStockData(shop);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("[{}] [GRAPHQL] Query completed - shopWithStock - Stock items: {} - Time: {}ms", 
                    traceId, stockItems.size(), executionTime);

            return new ShopWithStock(shop, stockItems, OffsetDateTime.now().toString());

        } catch (Exception e) {
            log.error("[{}] [GRAPHQL] Query failed - shopWithStock - Error: {}", traceId, e.getMessage());
            throw e;
        }
    }

    @QueryMapping
    public Order order(@Argument String id) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: order(id: {})", traceId, id);
        
        return orderRepository.findByIdWithItems(UUID.fromString(id)).orElse(null);
    }

    @QueryMapping
    public Order orderByNumber(@Argument String orderNumber) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: orderByNumber(orderNumber: {})", traceId, orderNumber);
        
        return orderRepository.findByOrderNumberWithItems(orderNumber).orElse(null);
    }

    @QueryMapping
    public OrderConnection ordersByShop(@Argument String shopId, @Argument Integer page, @Argument Integer size) {
        String traceId = correlationIdManager.getTraceId();
        MDC.put(CorrelationIdManager.API_TYPE_KEY, ApiType.GRAPHQL.getValue());
        
        log.info("[{}] [GRAPHQL] Query: ordersByShop(shopId: {}, page: {}, size: {})", traceId, shopId, page, size);

        PageRequest pageRequest = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        Page<Order> orderPage = orderRepository.findByShopId(UUID.fromString(shopId), pageRequest);

        return new OrderConnection(
                orderPage.getContent(),
                new PageInfo(
                        orderPage.getNumber(),
                        orderPage.getSize(),
                        orderPage.getTotalElements(),
                        orderPage.getTotalPages(),
                        orderPage.hasNext(),
                        orderPage.hasPrevious()
                )
        );
    }

    @SchemaMapping(typeName = "Shop", field = "openingHours")
    public List<OpeningHour> openingHours(Shop shop) {
        if (shop.getOpeningHours() == null) {
            return Collections.emptyList();
        }
        return shop.getOpeningHours().entrySet().stream()
                .map(entry -> new OpeningHour(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * Fetch live stock data from Product-Stock service.
     */
    private List<StockItem> fetchLiveStockData(Shop shop) {
        try {
            // In a real scenario, we would fetch products associated with this shop
            // For now, we'll fetch a sample of products
            List<UUID> productIds = getProductIdsForShop(shop);
            
            if (productIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<ProductResponse> products = productServiceClient.getProductsByIds(productIds);
            
            return products.stream()
                    .map(p -> new StockItem(
                            p.getId().toString(),
                            p.getProductCode(),
                            p.getName(),
                            p.getAvailableQuantity(),
                            p.getReservedQuantity(),
                            p.getStatus()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to fetch live stock data: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<UUID> getProductIdsForShop(Shop shop) {
        // In real implementation, this would fetch product IDs from cache or configuration
        return Collections.emptyList();
    }

    // GraphQL type records
    public record ShopConnection(List<Shop> content, PageInfo pageInfo) {}
    public record OrderConnection(List<Order> content, PageInfo pageInfo) {}
    public record PageInfo(int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
    public record ShopWithStock(Shop shop, List<StockItem> stockItems, String lastSyncedAt) {}
    public record StockItem(String productId, String productCode, String productName, int availableQuantity, int reservedQuantity, String status) {}
    public record OpeningHour(String day, String hours) {}
}
