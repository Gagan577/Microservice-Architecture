package com.enterprise.product.service;

import com.enterprise.product.dto.ProductDto;
import com.enterprise.product.entity.Product;
import com.enterprise.product.entity.Stock;
import com.enterprise.product.exception.DuplicateResourceException;
import com.enterprise.product.exception.ResourceNotFoundException;
import com.enterprise.product.repository.ProductRepository;
import com.enterprise.product.repository.StockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing products and their catalog information.
 */
@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;

    public ProductService(ProductRepository productRepository, StockRepository stockRepository) {
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    /**
     * Create a new product with initial stock.
     */
    public ProductDto.Response createProduct(ProductDto.CreateRequest request) {
        log.info("Creating product with code: {}", request.getProductCode());

        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new DuplicateResourceException("Product", request.getProductCode());
        }

        Product product = Product.builder()
                .productCode(request.getProductCode())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .price(request.getPrice())
                .weight(request.getWeight())
                .dimensions(request.getDimensions())
                .status(Product.ProductStatus.ACTIVE)
                .build();

        product = productRepository.save(product);

        // Create initial stock record
        Stock stock = Stock.builder()
                .productId(product.getId())
                .productCode(product.getProductCode())
                .availableQuantity(request.getInitialStock() != null ? request.getInitialStock() : 0)
                .reservedQuantity(0)
                .minimumStock(request.getMinimumStock() != null ? request.getMinimumStock() : 10)
                .maximumStock(request.getMaximumStock() != null ? request.getMaximumStock() : 1000)
                .reorderPoint(request.getReorderPoint() != null ? request.getReorderPoint() : 20)
                .warehouseLocation(request.getWarehouseLocation())
                .build();

        stockRepository.save(stock);

        log.info("Product created successfully with ID: {}", product.getId());
        return mapToResponse(product);
    }

    /**
     * Update an existing product.
     */
    public ProductDto.Response updateProduct(UUID id, ProductDto.UpdateRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getWeight() != null) {
            product.setWeight(request.getWeight());
        }
        if (request.getDimensions() != null) {
            product.setDimensions(request.getDimensions());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        product = productRepository.save(product);

        log.info("Product updated successfully with ID: {}", product.getId());
        return mapToResponse(product);
    }

    /**
     * Get a product by ID.
     */
    @Transactional(readOnly = true)
    public ProductDto.Response getProduct(UUID id) {
        log.debug("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        return mapToResponse(product);
    }

    /**
     * Get a product by product code.
     */
    @Transactional(readOnly = true)
    public ProductDto.Response getProductByCode(String productCode) {
        log.debug("Fetching product with code: {}", productCode);

        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productCode));

        return mapToResponse(product);
    }

    /**
     * Get all products with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto.Response> getAllProducts(Pageable pageable) {
        log.debug("Fetching all products with pagination");

        return productRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get products by category with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto.Response> getProductsByCategory(String category, Pageable pageable) {
        log.debug("Fetching products by category: {}", category);

        return productRepository.findByCategory(category, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get products by status with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto.Response> getProductsByStatus(Product.ProductStatus status, Pageable pageable) {
        log.debug("Fetching products by status: {}", status);

        return productRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Search products by name.
     */
    @Transactional(readOnly = true)
    public Page<ProductDto.Response> searchProducts(String name, Pageable pageable) {
        log.debug("Searching products by name: {}", name);

        return productRepository.searchByName(name, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Get all categories.
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories() {
        return productRepository.findAllCategories();
    }

    /**
     * Deactivate a product (soft delete).
     */
    public void deactivateProduct(UUID id) {
        log.info("Deactivating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id.toString()));

        product.setStatus(Product.ProductStatus.DISCONTINUED);
        productRepository.save(product);

        log.info("Product deactivated successfully with ID: {}", id);
    }

    /**
     * Get products by IDs (for batch queries).
     */
    @Transactional(readOnly = true)
    public List<ProductDto.Summary> getProductsByIds(List<UUID> ids) {
        log.debug("Fetching products by IDs: {}", ids);

        return productRepository.findByIdIn(ids).stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Get product statistics.
     */
    @Transactional(readOnly = true)
    public ProductDto.Statistics getProductStatistics() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByStatus(Product.ProductStatus.ACTIVE);
        long inactiveProducts = productRepository.countByStatus(Product.ProductStatus.INACTIVE);
        long discontinuedProducts = productRepository.countByStatus(Product.ProductStatus.DISCONTINUED);
        List<String> categories = productRepository.findAllCategories();

        return ProductDto.Statistics.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(inactiveProducts)
                .discontinuedProducts(discontinuedProducts)
                .totalCategories(categories.size())
                .categories(categories)
                .build();
    }

    private ProductDto.Response mapToResponse(Product product) {
        return ProductDto.Response.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private ProductDto.Summary mapToSummary(Product product) {
        return ProductDto.Summary.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .status(product.getStatus())
                .build();
    }
}
