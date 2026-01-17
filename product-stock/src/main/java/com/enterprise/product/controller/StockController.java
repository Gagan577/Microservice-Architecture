package com.enterprise.product.controller;

import com.enterprise.product.dto.ApiResponse;
import com.enterprise.product.dto.StockDto;
import com.enterprise.product.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Stock operations.
 */
@RestController
@RequestMapping("/api/v1/stock")
@Tag(name = "Stock", description = "Stock management APIs")
public class StockController {

    private static final Logger log = LoggerFactory.getLogger(StockController.class);

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get stock level", description = "Retrieves the current stock level for a product")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stock level retrieved",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found")
    })
    public ResponseEntity<ApiResponse<StockDto.StockLevelResponse>> getStockLevel(
            @Parameter(description = "Product ID") @PathVariable UUID productId) {

        log.debug("Getting stock level for product: {}", productId);
        StockDto.StockLevelResponse stock = stockService.getStockLevel(productId);

        return ResponseEntity.ok(ApiResponse.success(stock));
    }

    @PostMapping("/batch")
    @Operation(summary = "Get stock levels for multiple products", description = "Retrieves stock levels for multiple products")
    public ResponseEntity<ApiResponse<List<StockDto.StockLevelResponse>>> getStockLevels(
            @RequestBody List<UUID> productIds) {

        log.debug("Getting stock levels for products: {}", productIds);
        List<StockDto.StockLevelResponse> stocks = stockService.getStockLevels(productIds);

        return ResponseEntity.ok(ApiResponse.success(stocks));
    }

    @GetMapping("/{productId}/availability")
    @Operation(summary = "Check stock availability", description = "Checks if the requested quantity is available for a product")
    public ResponseEntity<ApiResponse<StockDto.AvailabilityResponse>> checkAvailability(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Parameter(description = "Required quantity") @RequestParam Integer quantity) {

        log.info("Checking availability for product {} with quantity {}", productId, quantity);
        StockDto.AvailabilityResponse availability = stockService.checkAvailability(productId, quantity);

        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    @PostMapping("/add")
    @Operation(summary = "Add stock", description = "Adds stock to a product (receiving inventory)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stock added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found")
    })
    public ResponseEntity<ApiResponse<StockDto.StockLevelResponse>> addStock(
            @Valid @RequestBody StockDto.UpdateStockRequest request) {

        log.info("Adding stock: {} units for product {}", request.getQuantity(), request.getProductId());
        StockDto.StockLevelResponse stock = stockService.addStock(request);

        return ResponseEntity.ok(ApiResponse.success(stock, "Stock added successfully"));
    }

    @PostMapping("/remove")
    @Operation(summary = "Remove stock", description = "Removes stock from a product (shipping, damage, etc.)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stock removed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Insufficient stock")
    })
    public ResponseEntity<ApiResponse<StockDto.StockLevelResponse>> removeStock(
            @Valid @RequestBody StockDto.UpdateStockRequest request) {

        log.info("Removing stock: {} units from product {}", request.getQuantity(), request.getProductId());
        StockDto.StockLevelResponse stock = stockService.removeStock(request);

        return ResponseEntity.ok(ApiResponse.success(stock, "Stock removed successfully"));
    }

    @PostMapping("/reserve")
    @Operation(summary = "Reserve stock", description = "Reserves stock for an order")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Stock reserved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Insufficient stock")
    })
    public ResponseEntity<ApiResponse<StockDto.ReservationResponse>> reserveStock(
            @Valid @RequestBody StockDto.ReserveStockRequest request) {

        log.info("Reserving stock: {} units for product {}, order {}",
                request.getQuantity(), request.getProductId(), request.getOrderId());
        StockDto.ReservationResponse reservation = stockService.reserveStock(request);

        return ResponseEntity.ok(ApiResponse.success(reservation, "Stock reserved successfully"));
    }

    @PostMapping("/reservation/{reservationCode}/confirm")
    @Operation(summary = "Confirm reservation", description = "Confirms a pending reservation when order is fulfilled")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reservation confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Reservation not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Reservation cannot be confirmed")
    })
    public ResponseEntity<ApiResponse<StockDto.ReservationResponse>> confirmReservation(
            @Parameter(description = "Reservation code") @PathVariable String reservationCode) {

        log.info("Confirming reservation: {}", reservationCode);
        StockDto.ReservationResponse reservation = stockService.confirmReservation(reservationCode);

        return ResponseEntity.ok(ApiResponse.success(reservation, "Reservation confirmed successfully"));
    }

    @PostMapping("/reservation/{reservationCode}/release")
    @Operation(summary = "Release reservation", description = "Releases a pending reservation (cancel order)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reservation released successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Reservation not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "Reservation cannot be released")
    })
    public ResponseEntity<ApiResponse<StockDto.ReservationResponse>> releaseReservation(
            @Parameter(description = "Reservation code") @PathVariable String reservationCode) {

        log.info("Releasing reservation: {}", reservationCode);
        StockDto.ReservationResponse reservation = stockService.releaseReservation(reservationCode);

        return ResponseEntity.ok(ApiResponse.success(reservation, "Reservation released successfully"));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock items", description = "Retrieves items below minimum stock threshold")
    public ResponseEntity<ApiResponse<List<StockDto.StockLevelResponse>>> getLowStockItems() {

        log.debug("Getting low stock items");
        List<StockDto.StockLevelResponse> lowStockItems = stockService.getLowStockItems();

        return ResponseEntity.ok(ApiResponse.success(lowStockItems));
    }

    @GetMapping("/out-of-stock")
    @Operation(summary = "Get out of stock items", description = "Retrieves items with zero available stock")
    public ResponseEntity<ApiResponse<List<StockDto.StockLevelResponse>>> getOutOfStockItems() {

        log.debug("Getting out of stock items");
        List<StockDto.StockLevelResponse> outOfStockItems = stockService.getOutOfStockItems();

        return ResponseEntity.ok(ApiResponse.success(outOfStockItems));
    }

    @PutMapping("/{productId}/thresholds")
    @Operation(summary = "Update stock thresholds", description = "Updates minimum, maximum, and reorder point for a product")
    public ResponseEntity<ApiResponse<StockDto.StockLevelResponse>> updateStockThresholds(
            @Parameter(description = "Product ID") @PathVariable UUID productId,
            @Parameter(description = "Minimum stock level") @RequestParam(required = false) Integer minimumStock,
            @Parameter(description = "Maximum stock level") @RequestParam(required = false) Integer maximumStock,
            @Parameter(description = "Reorder point") @RequestParam(required = false) Integer reorderPoint) {

        log.info("Updating stock thresholds for product: {}", productId);
        StockDto.StockLevelResponse stock = stockService.updateStockThresholds(
                productId, minimumStock, maximumStock, reorderPoint);

        return ResponseEntity.ok(ApiResponse.success(stock, "Stock thresholds updated successfully"));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get stock statistics", description = "Retrieves stock statistics across all products")
    public ResponseEntity<ApiResponse<StockDto.Statistics>> getStockStatistics() {

        log.debug("Getting stock statistics");
        StockDto.Statistics statistics = stockService.getStockStatistics();

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
