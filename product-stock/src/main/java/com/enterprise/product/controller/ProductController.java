package com.enterprise.product.controller;

import com.enterprise.product.dto.ApiResponse;
import com.enterprise.product.dto.ProductDto;
import com.enterprise.product.entity.Product;
import com.enterprise.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Product operations.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product", description = "Product management APIs")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product with initial stock")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Product already exists")
    })
    public ResponseEntity<ApiResponse<ProductDto.Response>> createProduct(
            @Valid @RequestBody ProductDto.CreateRequest request) {

        log.info("Creating product: {}", request.getProductCode());
        ProductDto.Response product = productService.createProduct(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique identifier")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {

        log.debug("Getting product: {}", id);
        ProductDto.Response product = productService.getProduct(id);

        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/code/{productCode}")
    @Operation(summary = "Get product by code", description = "Retrieves a product by its product code")
    public ResponseEntity<ApiResponse<ProductDto.Response>> getProductByCode(
            @Parameter(description = "Product code") @PathVariable String productCode) {

        log.debug("Getting product by code: {}", productCode);
        ProductDto.Response product = productService.getProductByCode(productCode);

        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves all products with pagination")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getAllProducts(
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting all products");
        Page<ProductDto.Response> products = productService.getAllProducts(pageable);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieves products filtered by category")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getProductsByCategory(
            @Parameter(description = "Product category") @PathVariable String category,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting products by category: {}", category);
        Page<ProductDto.Response> products = productService.getProductsByCategory(category, pageable);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get products by status", description = "Retrieves products filtered by status")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getProductsByStatus(
            @Parameter(description = "Product status") @PathVariable Product.ProductStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Getting products by status: {}", status);
        Page<ProductDto.Response> products = productService.getProductsByStatus(status, pageable);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by name")
    public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> searchProducts(
            @Parameter(description = "Search query") @RequestParam String q,
            @PageableDefault(size = 20) Pageable pageable) {

        log.debug("Searching products: {}", q);
        Page<ProductDto.Response> products = productService.searchProducts(q, pageable);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductDto.Response>> updateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Valid @RequestBody ProductDto.UpdateRequest request) {

        log.info("Updating product: {}", id);
        ProductDto.Response product = productService.updateProduct(id, request);

        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate product", description = "Deactivates a product (soft delete)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Product deactivated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {

        log.info("Deactivating product: {}", id);
        productService.deactivateProduct(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Product deactivated successfully"));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all categories", description = "Retrieves all product categories")
    public ResponseEntity<ApiResponse<List<String>>> getAllCategories() {

        log.debug("Getting all categories");
        List<String> categories = productService.getAllCategories();

        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @PostMapping("/batch")
    @Operation(summary = "Get products by IDs", description = "Retrieves multiple products by their IDs")
    public ResponseEntity<ApiResponse<List<ProductDto.Summary>>> getProductsByIds(
            @RequestBody List<UUID> ids) {

        log.debug("Getting products by IDs: {}", ids);
        List<ProductDto.Summary> products = productService.getProductsByIds(ids);

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get product statistics", description = "Retrieves product statistics")
    public ResponseEntity<ApiResponse<ProductDto.Statistics>> getProductStatistics() {

        log.debug("Getting product statistics");
        ProductDto.Statistics statistics = productService.getProductStatistics();

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
