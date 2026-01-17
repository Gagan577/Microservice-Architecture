package com.enterprise.shop.service;

import com.enterprise.shop.dto.OrderDto;
import com.enterprise.shop.entity.Order;
import com.enterprise.shop.entity.OrderItem;
import com.enterprise.shop.entity.Shop;
import com.enterprise.shop.exception.BusinessValidationException;
import com.enterprise.shop.exception.ResourceNotFoundException;
import com.enterprise.shop.exception.StockReservationException;
import com.enterprise.shop.logging.CorrelationIdManager;
import com.enterprise.shop.repository.OrderRepository;
import com.enterprise.shop.service.client.ProductServiceClient;
import com.enterprise.shop.service.client.dto.StockReservationRequest;
import com.enterprise.shop.service.client.dto.StockReservationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for Order management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ShopService shopService;
    private final ProductServiceClient productServiceClient;
    private final CorrelationIdManager correlationIdManager;

    private static final String ORDER_PREFIX = "ORD";

    /**
     * Place a new order with stock reservation.
     */
    @Transactional
    public OrderDto.Response placeOrder(OrderDto.PlaceOrderRequest request) {
        String traceId = correlationIdManager.getTraceId();
        log.info("[{}] Placing new order for shop: {}", traceId, request.getShopId());

        // Validate shop exists and is active
        Shop shop = shopService.getShopEntityById(request.getShopId());
        if (shop.getStatus() != Shop.ShopStatus.ACTIVE) {
            throw new BusinessValidationException("Cannot place order in inactive shop: " + shop.getShopCode());
        }

        // Generate order number
        String orderNumber = generateOrderNumber();
        log.debug("[{}] Generated order number: {}", traceId, orderNumber);

        // Create order entity
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .shop(shop)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .shippingAddress(mapAddressToMap(request.getShippingAddress()))
                .billingAddress(request.getBillingAddress() != null 
                        ? mapAddressToMap(request.getBillingAddress()) 
                        : mapAddressToMap(request.getShippingAddress()))
                .status(Order.OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(Order.PaymentStatus.PENDING)
                .notes(request.getNotes())
                .metadata(request.getMetadata())
                .build();

        // Create order items and reserve stock
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderDto.OrderItemRequest itemRequest : request.getItems()) {
            // Reserve stock via Product Service
            StockReservationResponse reservation = reserveStock(itemRequest, orderNumber, traceId);

            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productCode(itemRequest.getProductCode())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .lineTotal(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                    .stockReservationId(reservation.getReservationId())
                    .reservationStatus(reservation.isSuccess() 
                            ? OrderItem.ReservationStatus.RESERVED 
                            : OrderItem.ReservationStatus.FAILED)
                    .build();

            orderItems.add(item);
            order.addItem(item);
            subtotal = subtotal.add(item.getLineTotal());
        }

        // Calculate totals
        order.setSubtotal(subtotal);
        order.calculateTotals();

        // Update status based on reservation results
        boolean allReserved = orderItems.stream()
                .allMatch(item -> item.getReservationStatus() == OrderItem.ReservationStatus.RESERVED);

        if (allReserved) {
            order.setStatus(Order.OrderStatus.CONFIRMED);
            log.info("[{}] Order {} confirmed with all items reserved", traceId, orderNumber);
        } else {
            order.setStatus(Order.OrderStatus.PENDING);
            log.warn("[{}] Order {} has items with failed reservations", traceId, orderNumber);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("[{}] Order saved successfully: {} with {} items", 
                traceId, savedOrder.getOrderNumber(), savedOrder.getItems().size());

        return mapToResponse(savedOrder);
    }

    /**
     * Cancel an order and release reserved stock.
     */
    @Transactional
    public OrderDto.Response cancelOrder(UUID orderId, OrderDto.CancelOrderRequest request) {
        String traceId = correlationIdManager.getTraceId();
        log.info("[{}] Cancelling order: {}", traceId, orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Validate order can be cancelled
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BusinessValidationException("Order is already cancelled");
        }
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new BusinessValidationException("Cannot cancel delivered order");
        }
        if (order.getStatus() == Order.OrderStatus.SHIPPED) {
            throw new BusinessValidationException("Cannot cancel shipped order");
        }

        // Release stock reservations
        if (request.isReleaseStock()) {
            for (OrderItem item : order.getItems()) {
                if (item.getStockReservationId() != null && 
                    item.getReservationStatus() == OrderItem.ReservationStatus.RESERVED) {
                    
                    try {
                        productServiceClient.releaseStock(item.getStockReservationId());
                        item.setReservationStatus(OrderItem.ReservationStatus.RELEASED);
                        log.debug("[{}] Released stock for item: {}", traceId, item.getProductCode());
                    } catch (Exception e) {
                        log.error("[{}] Failed to release stock for item {}: {}", 
                                traceId, item.getProductCode(), e.getMessage());
                        // Continue with cancellation even if stock release fails
                    }
                }
            }
        }

        // Update order status
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(OffsetDateTime.now());
        order.setCancellationReason(request.getReason());

        Order savedOrder = orderRepository.save(order);
        log.info("[{}] Order {} cancelled successfully", traceId, savedOrder.getOrderNumber());

        return mapToResponse(savedOrder);
    }

    /**
     * Get order by ID.
     */
    @Transactional(readOnly = true)
    public OrderDto.Response getOrderById(UUID orderId) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching order: {}", traceId, orderId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        return mapToResponse(order);
    }

    /**
     * Get order by order number.
     */
    @Transactional(readOnly = true)
    public OrderDto.Response getOrderByNumber(String orderNumber) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching order by number: {}", traceId, orderNumber);

        Order order = orderRepository.findByOrderNumberWithItems(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderNumber));

        return mapToResponse(order);
    }

    /**
     * Get orders by shop.
     */
    @Transactional(readOnly = true)
    public Page<OrderDto.Summary> getOrdersByShop(UUID shopId, Pageable pageable) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching orders for shop: {}", traceId, shopId);

        return orderRepository.findByShopId(shopId, pageable).map(this::mapToSummary);
    }

    /**
     * Get orders by status.
     */
    @Transactional(readOnly = true)
    public Page<OrderDto.Summary> getOrdersByStatus(Order.OrderStatus status, Pageable pageable) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching orders by status: {}", traceId, status);

        return orderRepository.findByStatus(status, pageable).map(this::mapToSummary);
    }

    /**
     * Reserve stock for an order item.
     */
    private StockReservationResponse reserveStock(OrderDto.OrderItemRequest item, String orderNumber, String traceId) {
        log.debug("[{}] Reserving stock for product: {} quantity: {}", 
                traceId, item.getProductCode(), item.getQuantity());

        try {
            StockReservationRequest reservationRequest = StockReservationRequest.builder()
                    .productId(item.getProductId())
                    .productCode(item.getProductCode())
                    .quantity(item.getQuantity())
                    .orderReference(orderNumber)
                    .build();

            return productServiceClient.reserveStock(reservationRequest);

        } catch (Exception e) {
            log.error("[{}] Stock reservation failed for product {}: {}", 
                    traceId, item.getProductCode(), e.getMessage());
            throw new StockReservationException("Failed to reserve stock for " + item.getProductCode());
        }
    }

    /**
     * Generate unique order number.
     */
    private String generateOrderNumber() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return ORDER_PREFIX + "-" + timestamp + "-" + random;
    }

    /**
     * Map address DTO to Map.
     */
    private Map<String, String> mapAddressToMap(OrderDto.AddressDto address) {
        Map<String, String> map = new HashMap<>();
        map.put("street", address.getStreet());
        map.put("apartment", address.getApartment());
        map.put("city", address.getCity());
        map.put("state", address.getState());
        map.put("postalCode", address.getPostalCode());
        map.put("country", address.getCountry());
        return map;
    }

    /**
     * Map Order entity to Response DTO.
     */
    private OrderDto.Response mapToResponse(Order order) {
        List<OrderDto.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapItemToResponse)
                .collect(Collectors.toList());

        return OrderDto.Response.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .shopId(order.getShop().getId())
                .shopCode(order.getShop().getShopCode())
                .shopName(order.getShop().getName())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .shippingAmount(order.getShippingAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .notes(order.getNotes())
                .items(itemResponses)
                .orderedAt(order.getOrderedAt())
                .shippedAt(order.getShippedAt())
                .deliveredAt(order.getDeliveredAt())
                .cancelledAt(order.getCancelledAt())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * Map OrderItem to Response DTO.
     */
    private OrderDto.OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderDto.OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productCode(item.getProductCode())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountPercent(item.getDiscountPercent())
                .discountAmount(item.getDiscountAmount())
                .taxPercent(item.getTaxPercent())
                .taxAmount(item.getTaxAmount())
                .lineTotal(item.getLineTotal())
                .stockReservationId(item.getStockReservationId())
                .reservationStatus(item.getReservationStatus())
                .build();
    }

    /**
     * Map Order to Summary DTO.
     */
    private OrderDto.Summary mapToSummary(Order order) {
        return OrderDto.Summary.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .shopCode(order.getShop().getShopCode())
                .customerName(order.getCustomerName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getItems().size())
                .orderedAt(order.getOrderedAt())
                .build();
    }
}
