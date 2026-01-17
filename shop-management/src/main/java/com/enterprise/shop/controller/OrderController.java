package com.enterprise.shop.controller;

import com.enterprise.shop.dto.ApiResponse;
import com.enterprise.shop.dto.OrderDto;
import com.enterprise.shop.entity.Order;
import com.enterprise.shop.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * REST Controller for Order management operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Place a new order", description = "Places a new order with stock reservation")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order placed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Shop not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Stock reservation failed")
    })
    public ResponseEntity<ApiResponse.Response<OrderDto.Response>> placeOrder(
            @Valid @RequestBody OrderDto.PlaceOrderRequest request) {

        log.info("Received request to place order for shop: {}", request.getShopId());
        OrderDto.Response order = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.Response.success(order, "Order placed successfully"));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order", description = "Cancels an order and releases reserved stock")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order cannot be cancelled")
    })
    public ResponseEntity<ApiResponse.Response<OrderDto.Response>> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable UUID orderId,
            @Valid @RequestBody OrderDto.CancelOrderRequest request) {

        log.info("Received request to cancel order: {}", orderId);
        OrderDto.Response order = orderService.cancelOrder(orderId, request);
        return ResponseEntity.ok(ApiResponse.Response.success(order, "Order cancelled successfully"));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieves order details by its unique identifier")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse.Response<OrderDto.Response>> getOrderById(
            @Parameter(description = "Order ID") @PathVariable UUID orderId) {

        log.debug("Received request to get order: {}", orderId);
        OrderDto.Response order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.Response.success(order));
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by number", description = "Retrieves order details by order number")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse.Response<OrderDto.Response>> getOrderByNumber(
            @Parameter(description = "Order number") @PathVariable String orderNumber) {

        log.debug("Received request to get order by number: {}", orderNumber);
        OrderDto.Response order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.Response.success(order));
    }

    @GetMapping("/shop/{shopId}")
    @Operation(summary = "Get orders by shop", description = "Retrieves orders for a specific shop")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    public ResponseEntity<ApiResponse.PagedResponse<OrderDto.Summary>> getOrdersByShop(
            @Parameter(description = "Shop ID") @PathVariable UUID shopId,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Received request to get orders for shop: {}", shopId);
        Page<OrderDto.Summary> orders = orderService.getOrdersByShop(shopId, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.of(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieves orders filtered by status")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully")
    })
    public ResponseEntity<ApiResponse.PagedResponse<OrderDto.Summary>> getOrdersByStatus(
            @Parameter(description = "Order status") @PathVariable Order.OrderStatus status,
            @PageableDefault(size = 20, sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.debug("Received request to get orders by status: {}", status);
        Page<OrderDto.Summary> orders = orderService.getOrdersByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.PagedResponse.of(
                orders.getContent(),
                orders.getNumber(),
                orders.getSize(),
                orders.getTotalElements(),
                orders.getTotalPages()));
    }
}
