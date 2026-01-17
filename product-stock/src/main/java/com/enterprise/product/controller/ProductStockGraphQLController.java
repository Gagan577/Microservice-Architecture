package com.enterprise.product.controller;

import com.enterprise.product.dto.ProductDto;
import com.enterprise.product.dto.StockDto;
import com.enterprise.product.entity.Product;
import com.enterprise.product.logging.ApiType;
import com.enterprise.product.logging.CorrelationIdManager;
import com.enterprise.product.service.ProductService;
import com.enterprise.product.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Controller for Product-Stock service.
 * Provides GraphQL queries and mutations for products and stock.
 */
@Controller
public class ProductStockGraphQLController {

    private static final Logger log = LoggerFactory.getLogger(ProductStockGraphQLController.class);

    private final ProductService productService;
    private final StockService stockService;

    public ProductStockGraphQLController(ProductService productService, StockService stockService) {
        this.productService = productService;
        this.stockService = stockService;
    }

    // ==================== Product Queries ====================

    @QueryMapping
    public ProductDto.Response product(@Argument String id) {
        setupGraphQLContext();
        log.debug("GraphQL query: product({})", id);
        return productService.getProduct(UUID.fromString(id));
    }

    @QueryMapping
    public ProductDto.Response productByCode(@Argument String productCode) {
        setupGraphQLContext();
        log.debug("GraphQL query: productByCode({})", productCode);
        return productService.getProductByCode(productCode);
    }

    @QueryMapping
    public Map<String, Object> products(@Argument Integer page, @Argument Integer size) {
        setupGraphQLContext();
        log.debug("GraphQL query: products(page={}, size={})", page, size);
        Page<ProductDto.Response> pageResult = productService.getAllProducts(PageRequest.of(page, size));
        return toPageMap(pageResult, page, size);
    }

    @QueryMapping
    public Map<String, Object> productsByCategory(@Argument String category,
                                                   @Argument Integer page,
                                                   @Argument Integer size) {
        setupGraphQLContext();
        log.debug("GraphQL query: productsByCategory({}, page={}, size={})", category, page, size);
        Page<ProductDto.Response> pageResult = productService.getProductsByCategory(
                category, PageRequest.of(page, size));
        return toPageMap(pageResult, page, size);
    }

    @QueryMapping
    public Map<String, Object> searchProducts(@Argument String query,
                                               @Argument Integer page,
                                               @Argument Integer size) {
        setupGraphQLContext();
        log.debug("GraphQL query: searchProducts({}, page={}, size={})", query, page, size);
        Page<ProductDto.Response> pageResult = productService.searchProducts(
                query, PageRequest.of(page, size));
        return toPageMap(pageResult, page, size);
    }

    @QueryMapping
    public List<String> productCategories() {
        setupGraphQLContext();
        log.debug("GraphQL query: productCategories()");
        return productService.getAllCategories();
    }

    @QueryMapping
    public ProductDto.Statistics productStatistics() {
        setupGraphQLContext();
        log.debug("GraphQL query: productStatistics()");
        return productService.getProductStatistics();
    }

    // ==================== Stock Queries ====================

    @QueryMapping
    public StockDto.StockLevelResponse stockLevel(@Argument String productId) {
        setupGraphQLContext();
        log.debug("GraphQL query: stockLevel({})", productId);
        return stockService.getStockLevel(UUID.fromString(productId));
    }

    @QueryMapping
    public List<StockDto.StockLevelResponse> stockLevels(@Argument List<String> productIds) {
        setupGraphQLContext();
        log.debug("GraphQL query: stockLevels({})", productIds);
        List<UUID> ids = productIds.stream().map(UUID::fromString).toList();
        return stockService.getStockLevels(ids);
    }

    @QueryMapping
    public StockDto.AvailabilityResponse checkAvailability(@Argument String productId,
                                                            @Argument Integer quantity) {
        setupGraphQLContext();
        log.debug("GraphQL query: checkAvailability({}, {})", productId, quantity);
        return stockService.checkAvailability(UUID.fromString(productId), quantity);
    }

    @QueryMapping
    public List<StockDto.StockLevelResponse> lowStockItems() {
        setupGraphQLContext();
        log.debug("GraphQL query: lowStockItems()");
        return stockService.getLowStockItems();
    }

    @QueryMapping
    public List<StockDto.StockLevelResponse> outOfStockItems() {
        setupGraphQLContext();
        log.debug("GraphQL query: outOfStockItems()");
        return stockService.getOutOfStockItems();
    }

    @QueryMapping
    public StockDto.Statistics stockStatistics() {
        setupGraphQLContext();
        log.debug("GraphQL query: stockStatistics()");
        return stockService.getStockStatistics();
    }

    // ==================== Combined Query ====================

    @QueryMapping
    public Map<String, Object> productWithStock(@Argument String productId) {
        setupGraphQLContext();
        log.debug("GraphQL query: productWithStock({})", productId);
        
        UUID id = UUID.fromString(productId);
        ProductDto.Response product = productService.getProduct(id);
        StockDto.StockLevelResponse stock = stockService.getStockLevel(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("product", product);
        result.put("stock", stock);
        return result;
    }

    // ==================== Product Mutations ====================

    @MutationMapping
    public ProductDto.Response createProduct(@Argument Map<String, Object> input) {
        setupGraphQLContext();
        log.info("GraphQL mutation: createProduct({})", input.get("productCode"));

        ProductDto.CreateRequest request = new ProductDto.CreateRequest();
        request.setProductCode((String) input.get("productCode"));
        request.setName((String) input.get("name"));
        request.setDescription((String) input.get("description"));
        request.setCategory((String) input.get("category"));
        request.setPrice(toBigDecimal(input.get("price")));
        request.setWeight(toBigDecimal(input.get("weight")));
        request.setDimensions((String) input.get("dimensions"));
        request.setInitialStock((Integer) input.get("initialStock"));
        request.setMinimumStock((Integer) input.get("minimumStock"));
        request.setMaximumStock((Integer) input.get("maximumStock"));
        request.setReorderPoint((Integer) input.get("reorderPoint"));
        request.setWarehouseLocation((String) input.get("warehouseLocation"));

        return productService.createProduct(request);
    }

    @MutationMapping
    public ProductDto.Response updateProduct(@Argument String id, @Argument Map<String, Object> input) {
        setupGraphQLContext();
        log.info("GraphQL mutation: updateProduct({})", id);

        ProductDto.UpdateRequest request = new ProductDto.UpdateRequest();
        if (input.containsKey("name")) request.setName((String) input.get("name"));
        if (input.containsKey("description")) request.setDescription((String) input.get("description"));
        if (input.containsKey("category")) request.setCategory((String) input.get("category"));
        if (input.containsKey("price")) request.setPrice(toBigDecimal(input.get("price")));
        if (input.containsKey("weight")) request.setWeight(toBigDecimal(input.get("weight")));
        if (input.containsKey("dimensions")) request.setDimensions((String) input.get("dimensions"));
        if (input.containsKey("status")) {
            request.setStatus(Product.ProductStatus.valueOf((String) input.get("status")));
        }

        return productService.updateProduct(UUID.fromString(id), request);
    }

    @MutationMapping
    public Boolean deactivateProduct(@Argument String id) {
        setupGraphQLContext();
        log.info("GraphQL mutation: deactivateProduct({})", id);
        productService.deactivateProduct(UUID.fromString(id));
        return true;
    }

    // ==================== Stock Mutations ====================

    @MutationMapping
    public StockDto.StockLevelResponse addStock(@Argument Map<String, Object> input) {
        setupGraphQLContext();
        log.info("GraphQL mutation: addStock({})", input);

        StockDto.UpdateStockRequest request = new StockDto.UpdateStockRequest();
        request.setProductId(UUID.fromString((String) input.get("productId")));
        request.setQuantity((Integer) input.get("quantity"));
        request.setReference((String) input.get("reference"));
        request.setReason((String) input.get("reason"));

        return stockService.addStock(request);
    }

    @MutationMapping
    public StockDto.StockLevelResponse removeStock(@Argument Map<String, Object> input) {
        setupGraphQLContext();
        log.info("GraphQL mutation: removeStock({})", input);

        StockDto.UpdateStockRequest request = new StockDto.UpdateStockRequest();
        request.setProductId(UUID.fromString((String) input.get("productId")));
        request.setQuantity((Integer) input.get("quantity"));
        request.setReference((String) input.get("reference"));
        request.setReason((String) input.get("reason"));

        return stockService.removeStock(request);
    }

    @MutationMapping
    public StockDto.ReservationResponse reserveStock(@Argument Map<String, Object> input) {
        setupGraphQLContext();
        log.info("GraphQL mutation: reserveStock({})", input);

        StockDto.ReserveStockRequest request = new StockDto.ReserveStockRequest();
        request.setProductId(UUID.fromString((String) input.get("productId")));
        request.setOrderId(UUID.fromString((String) input.get("orderId")));
        request.setQuantity((Integer) input.get("quantity"));
        request.setReservationMinutes((Integer) input.get("reservationMinutes"));

        return stockService.reserveStock(request);
    }

    @MutationMapping
    public StockDto.ReservationResponse confirmReservation(@Argument String reservationCode) {
        setupGraphQLContext();
        log.info("GraphQL mutation: confirmReservation({})", reservationCode);
        return stockService.confirmReservation(reservationCode);
    }

    @MutationMapping
    public StockDto.ReservationResponse releaseReservation(@Argument String reservationCode) {
        setupGraphQLContext();
        log.info("GraphQL mutation: releaseReservation({})", reservationCode);
        return stockService.releaseReservation(reservationCode);
    }

    @MutationMapping
    public StockDto.StockLevelResponse updateStockThresholds(@Argument String productId,
                                                              @Argument Integer minimumStock,
                                                              @Argument Integer maximumStock,
                                                              @Argument Integer reorderPoint) {
        setupGraphQLContext();
        log.info("GraphQL mutation: updateStockThresholds({})", productId);
        return stockService.updateStockThresholds(
                UUID.fromString(productId), minimumStock, maximumStock, reorderPoint);
    }

    // ==================== Helper Methods ====================

    private void setupGraphQLContext() {
        if (CorrelationIdManager.getCorrelationId() == null) {
            CorrelationIdManager.setupCorrelation(null, ApiType.GRAPHQL);
        }
    }

    private Map<String, Object> toPageMap(Page<?> page, int pageNum, int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", page.getContent());
        result.put("totalElements", page.getTotalElements());
        result.put("totalPages", page.getTotalPages());
        result.put("page", pageNum);
        result.put("size", size);
        return result;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }
}
