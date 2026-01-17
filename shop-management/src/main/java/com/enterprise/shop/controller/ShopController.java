package com.enterprise.shop.controller;

import com.enterprise.shop.dto.ApiResponse;
import com.enterprise.shop.dto.ShopDto;
import com.enterprise.shop.entity.Shop;
import com.enterprise.shop.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Shop management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/shops")
@RequiredArgsConstructor
@Tag(name = "Shop Management", description = "APIs for managing shops")
public class ShopController {

    private final ShopService shopService;

    @PostMapping
    @Operation(summary = "Create a new shop", description = "Creates a new shop with the provided details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Shop created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Shop with same code already exists")
    })
    public ResponseEntity<ApiResponse.Response<ShopDto.Response>> createShop(
            @Valid @RequestBody ShopDto.CreateRequest request) {

        log.info("Received request to create shop: {}", request.getShopCode());
        ShopDto.Response shop = shopService.createShop(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Response.success(shop, "Shop created successfully"));
    }

    @PutMapping("/{shopId}")
    @Operation(summary = "Update shop details", description = "Updates an existing shop's details")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shop updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shop not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<ApiResponse.Response<ShopDto.Response>> updateShop(
            @Parameter(description = "Shop ID") @PathVariable UUID shopId,
            @Valid @RequestBody ShopDto.UpdateRequest request) {

        log.info("Received request to update shop: {}", shopId);
        ShopDto.Response shop = shopService.updateShop(shopId, request);
        return ResponseEntity.ok(ApiResponse.Response.success(shop, "Shop updated successfully"));
    }

    @GetMapping("/{shopId}")
    @Operation(summary = "Get shop by ID", description = "Retrieves shop details by its unique identifier")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shop retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<ApiResponse.Response<ShopDto.Response>> getShopById(
            @Parameter(description = "Shop ID") @PathVariable UUID shopId) {

        log.debug("Received request to get shop: {}", shopId);
        ShopDto.Response shop = shopService.getShopById(shopId);
        return ResponseEntity.ok(ApiResponse.Response.success(shop));
    }

    @GetMapping("/code/{shopCode}")
    @Operation(summary = "Get shop by code", description = "Retrieves shop details by its unique code")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shop retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<ApiResponse.Response<ShopDto.Response>> getShopByCode(
            @Parameter(description = "Shop code") @PathVariable String shopCode) {

        log.debug("Received request to get shop by code: {}", shopCode);
        ShopDto.Response shop = shopService.getShopByCode(shopCode);
        return ResponseEntity.ok(ApiResponse.Response.success(shop));
    }

    @GetMapping
    @Operation(summary = "Get all shops", description = "Retrieves all shops with pagination")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shops retrieved successfully")
    })
    public ResponseEntity<ApiResponse.PagedResponse<ShopDto.Summary>> getAllShops(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Received request to get all shops - page: {}", pageable.getPageNumber());
        Page<ShopDto.Summary> shops = shopService.getAllShops(pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.of(
                shops.getContent(),
                shops.getNumber(),
                shops.getSize(),
                shops.getTotalElements(),
                shops.getTotalPages()));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get shops by status", description = "Retrieves shops filtered by status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Shops retrieved successfully")
    })
    public ResponseEntity<ApiResponse.PagedResponse<ShopDto.Summary>> getShopsByStatus(
            @Parameter(description = "Shop status") @PathVariable Shop.ShopStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Received request to get shops by status: {}", status);
        Page<ShopDto.Summary> shops = shopService.getShopsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.of(
                shops.getContent(),
                shops.getNumber(),
                shops.getSize(),
                shops.getTotalElements(),
                shops.getTotalPages()));
    }

    @GetMapping("/search")
    @Operation(summary = "Search shops", description = "Search shops by name")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results retrieved")
    })
    public ResponseEntity<ApiResponse.PagedResponse<ShopDto.Summary>> searchShops(
            @Parameter(description = "Search query") @RequestParam String name,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        log.debug("Received request to search shops with name: {}", name);
        Page<ShopDto.Summary> shops = shopService.searchShops(name, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.of(
                shops.getContent(),
                shops.getNumber(),
                shops.getSize(),
                shops.getTotalElements(),
                shops.getTotalPages()));
    }
}
