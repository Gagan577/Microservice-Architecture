package com.enterprise.product.service;

import com.enterprise.product.dto.StockDto;
import com.enterprise.product.entity.Product;
import com.enterprise.product.entity.Stock;
import com.enterprise.product.entity.StockMovement;
import com.enterprise.product.entity.StockReservation;
import com.enterprise.product.exception.BusinessValidationException;
import com.enterprise.product.exception.InsufficientStockException;
import com.enterprise.product.exception.ResourceNotFoundException;
import com.enterprise.product.exception.StockReservationException;
import com.enterprise.product.repository.ProductRepository;
import com.enterprise.product.repository.StockMovementRepository;
import com.enterprise.product.repository.StockRepository;
import com.enterprise.product.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing stock levels, reservations, and movements.
 */
@Service
@Transactional
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);
    private static final int DEFAULT_RESERVATION_MINUTES = 30;

    private final StockRepository stockRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public StockService(StockRepository stockRepository,
                        StockReservationRepository reservationRepository,
                        StockMovementRepository movementRepository,
                        ProductRepository productRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    /**
     * Get stock level for a product.
     */
    @Transactional(readOnly = true)
    public StockDto.StockLevelResponse getStockLevel(UUID productId) {
        log.debug("Getting stock level for product: {}", productId);

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", productId.toString()));

        return mapToStockLevel(stock);
    }

    /**
     * Get stock levels for multiple products.
     */
    @Transactional(readOnly = true)
    public List<StockDto.StockLevelResponse> getStockLevels(List<UUID> productIds) {
        log.debug("Getting stock levels for products: {}", productIds);

        return stockRepository.findByProductIdIn(productIds).stream()
                .map(this::mapToStockLevel)
                .collect(Collectors.toList());
    }

    /**
     * Check stock availability for a product (SOAP + REST endpoint).
     */
    @Transactional(readOnly = true)
    public StockDto.AvailabilityResponse checkAvailability(UUID productId, Integer requiredQuantity) {
        log.info("Checking availability for product {} with quantity {}", productId, requiredQuantity);

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", productId.toString()));

        boolean isAvailable = stock.getAvailableQuantity() >= requiredQuantity;

        return StockDto.AvailabilityResponse.builder()
                .productId(productId)
                .productCode(stock.getProductCode())
                .requestedQuantity(requiredQuantity)
                .availableQuantity(stock.getAvailableQuantity())
                .isAvailable(isAvailable)
                .message(isAvailable ? "Stock available" : "Insufficient stock")
                .checkedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Add stock (receiving inventory).
     */
    public StockDto.StockLevelResponse addStock(StockDto.UpdateStockRequest request) {
        log.info("Adding {} units to stock for product {}", request.getQuantity(), request.getProductId());

        Stock stock = stockRepository.findByProductIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", request.getProductId().toString()));

        int previousQuantity = stock.getAvailableQuantity();
        stock.setAvailableQuantity(stock.getAvailableQuantity() + request.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        stock = stockRepository.save(stock);

        // Record movement
        recordMovement(stock.getProductId(), stock.getProductCode(),
                StockMovement.MovementType.STOCK_IN, request.getQuantity(),
                previousQuantity, stock.getAvailableQuantity(),
                request.getReference(), request.getReason());

        log.info("Stock added successfully. New available: {}", stock.getAvailableQuantity());
        return mapToStockLevel(stock);
    }

    /**
     * Remove stock (shipping, damage, etc.).
     */
    public StockDto.StockLevelResponse removeStock(StockDto.UpdateStockRequest request) {
        log.info("Removing {} units from stock for product {}", request.getQuantity(), request.getProductId());

        Stock stock = stockRepository.findByProductIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", request.getProductId().toString()));

        if (stock.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    request.getProductId().toString(),
                    request.getQuantity(),
                    stock.getAvailableQuantity());
        }

        int previousQuantity = stock.getAvailableQuantity();
        stock.setAvailableQuantity(stock.getAvailableQuantity() - request.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        stock = stockRepository.save(stock);

        // Record movement
        recordMovement(stock.getProductId(), stock.getProductCode(),
                StockMovement.MovementType.STOCK_OUT, request.getQuantity(),
                previousQuantity, stock.getAvailableQuantity(),
                request.getReference(), request.getReason());

        log.info("Stock removed successfully. New available: {}", stock.getAvailableQuantity());
        return mapToStockLevel(stock);
    }

    /**
     * Reserve stock for an order.
     */
    public StockDto.ReservationResponse reserveStock(StockDto.ReserveStockRequest request) {
        log.info("Reserving {} units for product {}, order {}",
                request.getQuantity(), request.getProductId(), request.getOrderId());

        Stock stock = stockRepository.findByProductIdWithLock(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", request.getProductId().toString()));

        if (stock.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    request.getProductId().toString(),
                    request.getQuantity(),
                    stock.getAvailableQuantity());
        }

        // Update stock
        int previousAvailable = stock.getAvailableQuantity();
        stock.setAvailableQuantity(stock.getAvailableQuantity() - request.getQuantity());
        stock.setReservedQuantity(stock.getReservedQuantity() + request.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

        // Create reservation
        String reservationCode = "RSV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int reservationMinutes = request.getReservationMinutes() != null ?
                request.getReservationMinutes() : DEFAULT_RESERVATION_MINUTES;

        StockReservation reservation = StockReservation.builder()
                .reservationCode(reservationCode)
                .productId(request.getProductId())
                .productCode(stock.getProductCode())
                .orderId(request.getOrderId())
                .quantity(request.getQuantity())
                .status(StockReservation.ReservationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(reservationMinutes))
                .build();

        reservation = reservationRepository.save(reservation);

        // Record movement
        recordMovement(stock.getProductId(), stock.getProductCode(),
                StockMovement.MovementType.RESERVATION, request.getQuantity(),
                previousAvailable, stock.getAvailableQuantity(),
                reservationCode, "Stock reserved for order: " + request.getOrderId());

        log.info("Stock reserved successfully. Reservation code: {}", reservationCode);

        return StockDto.ReservationResponse.builder()
                .reservationId(reservation.getId())
                .reservationCode(reservationCode)
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .status(StockReservation.ReservationStatus.PENDING.name())
                .expiresAt(reservation.getExpiresAt())
                .message("Stock reserved successfully")
                .build();
    }

    /**
     * Confirm a reservation (when order is fulfilled).
     */
    public StockDto.ReservationResponse confirmReservation(String reservationCode) {
        log.info("Confirming reservation: {}", reservationCode);

        StockReservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationCode));

        if (reservation.getStatus() != StockReservation.ReservationStatus.PENDING) {
            throw new StockReservationException(reservationCode,
                    "Reservation is not in PENDING status. Current status: " + reservation.getStatus());
        }

        Stock stock = stockRepository.findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", reservation.getProductId().toString()));

        // Update stock
        stock.setReservedQuantity(stock.getReservedQuantity() - reservation.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

        // Update reservation
        reservation.setStatus(StockReservation.ReservationStatus.CONFIRMED);
        reservation.setConfirmedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        // Record movement
        recordMovement(stock.getProductId(), stock.getProductCode(),
                StockMovement.MovementType.RESERVATION_CONFIRMED, reservation.getQuantity(),
                null, null,
                reservationCode, "Reservation confirmed");

        log.info("Reservation confirmed: {}", reservationCode);

        return StockDto.ReservationResponse.builder()
                .reservationId(reservation.getId())
                .reservationCode(reservationCode)
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .status(StockReservation.ReservationStatus.CONFIRMED.name())
                .confirmedAt(reservation.getConfirmedAt())
                .message("Reservation confirmed successfully")
                .build();
    }

    /**
     * Release a reservation (cancel order).
     */
    public StockDto.ReservationResponse releaseReservation(String reservationCode) {
        log.info("Releasing reservation: {}", reservationCode);

        StockReservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationCode));

        if (reservation.getStatus() != StockReservation.ReservationStatus.PENDING) {
            throw new StockReservationException(reservationCode,
                    "Reservation is not in PENDING status. Current status: " + reservation.getStatus());
        }

        Stock stock = stockRepository.findByProductIdWithLock(reservation.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock", reservation.getProductId().toString()));

        // Return stock
        int previousAvailable = stock.getAvailableQuantity();
        stock.setAvailableQuantity(stock.getAvailableQuantity() + reservation.getQuantity());
        stock.setReservedQuantity(stock.getReservedQuantity() - reservation.getQuantity());
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

        // Update reservation
        reservation.setStatus(StockReservation.ReservationStatus.RELEASED);
        reservation.setReleasedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        // Record movement
        recordMovement(stock.getProductId(), stock.getProductCode(),
                StockMovement.MovementType.RESERVATION_RELEASED, reservation.getQuantity(),
                previousAvailable, stock.getAvailableQuantity(),
                reservationCode, "Reservation released");

        log.info("Reservation released: {}", reservationCode);

        return StockDto.ReservationResponse.builder()
                .reservationId(reservation.getId())
                .reservationCode(reservationCode)
                .productId(reservation.getProductId())
                .quantity(reservation.getQuantity())
                .status(StockReservation.ReservationStatus.RELEASED.name())
                .releasedAt(reservation.getReleasedAt())
                .message("Reservation released successfully")
                .build();
    }

    /**
     * Get low stock items (below minimum threshold).
     */
    @Transactional(readOnly = true)
    public List<StockDto.StockLevelResponse> getLowStockItems() {
        log.debug("Getting low stock items");

        return stockRepository.findLowStockItems().stream()
                .map(this::mapToStockLevel)
                .collect(Collectors.toList());
    }

    /**
     * Get out of stock items.
     */
    @Transactional(readOnly = true)
    public List<StockDto.StockLevelResponse> getOutOfStockItems() {
        log.debug("Getting out of stock items");

        return stockRepository.findOutOfStockItems().stream()
                .map(this::mapToStockLevel)
                .collect(Collectors.toList());
    }

    /**
     * Update stock thresholds.
     */
    public StockDto.StockLevelResponse updateStockThresholds(UUID productId, Integer minimumStock,
                                                              Integer maximumStock, Integer reorderPoint) {
        log.info("Updating stock thresholds for product: {}", productId);

        Stock stock = stockRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock", productId.toString()));

        if (minimumStock != null) {
            stock.setMinimumStock(minimumStock);
        }
        if (maximumStock != null) {
            stock.setMaximumStock(maximumStock);
        }
        if (reorderPoint != null) {
            stock.setReorderPoint(reorderPoint);
        }

        stock.setLastUpdated(LocalDateTime.now());
        stock = stockRepository.save(stock);

        return mapToStockLevel(stock);
    }

    /**
     * Scheduled job to expire old reservations.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void expireOldReservations() {
        log.debug("Running reservation expiration job");

        List<StockReservation> expiredReservations =
                reservationRepository.findExpiredReservations(LocalDateTime.now());

        for (StockReservation reservation : expiredReservations) {
            try {
                Stock stock = stockRepository.findByProductIdWithLock(reservation.getProductId())
                        .orElse(null);

                if (stock != null) {
                    // Return stock
                    stock.setAvailableQuantity(stock.getAvailableQuantity() + reservation.getQuantity());
                    stock.setReservedQuantity(stock.getReservedQuantity() - reservation.getQuantity());
                    stock.setLastUpdated(LocalDateTime.now());
                    stockRepository.save(stock);

                    // Record movement
                    recordMovement(stock.getProductId(), stock.getProductCode(),
                            StockMovement.MovementType.RESERVATION_EXPIRED, reservation.getQuantity(),
                            null, stock.getAvailableQuantity(),
                            reservation.getReservationCode(), "Reservation expired");
                }

                // Update reservation
                reservation.setStatus(StockReservation.ReservationStatus.EXPIRED);
                reservation.setReleasedAt(LocalDateTime.now());
                reservationRepository.save(reservation);

                log.info("Expired reservation: {}", reservation.getReservationCode());
            } catch (Exception e) {
                log.error("Failed to expire reservation: {}", reservation.getReservationCode(), e);
            }
        }

        if (!expiredReservations.isEmpty()) {
            log.info("Expired {} reservations", expiredReservations.size());
        }
    }

    /**
     * Get stock statistics.
     */
    @Transactional(readOnly = true)
    public StockDto.Statistics getStockStatistics() {
        Long totalAvailable = stockRepository.getTotalAvailableStock();
        Long totalReserved = stockRepository.getTotalReservedStock();
        long lowStockCount = stockRepository.findLowStockItems().size();
        long outOfStockCount = stockRepository.findOutOfStockItems().size();
        long pendingReservations = reservationRepository.countByStatus(StockReservation.ReservationStatus.PENDING);

        return StockDto.Statistics.builder()
                .totalAvailableStock(totalAvailable != null ? totalAvailable : 0L)
                .totalReservedStock(totalReserved != null ? totalReserved : 0L)
                .lowStockItemCount(lowStockCount)
                .outOfStockItemCount(outOfStockCount)
                .pendingReservationsCount(pendingReservations)
                .build();
    }

    private void recordMovement(UUID productId, String productCode,
                                StockMovement.MovementType type, Integer quantity,
                                Integer previousQuantity, Integer newQuantity,
                                String reference, String notes) {
        StockMovement movement = StockMovement.builder()
                .productId(productId)
                .productCode(productCode)
                .movementType(type)
                .quantity(quantity)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .reference(reference)
                .notes(notes)
                .build();

        movementRepository.save(movement);
    }

    private StockDto.StockLevelResponse mapToStockLevel(Stock stock) {
        boolean isLowStock = stock.getAvailableQuantity() < stock.getMinimumStock();
        boolean needsReorder = stock.getAvailableQuantity() <= stock.getReorderPoint();

        return StockDto.StockLevelResponse.builder()
                .id(stock.getId())
                .productId(stock.getProductId())
                .productCode(stock.getProductCode())
                .availableQuantity(stock.getAvailableQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .totalQuantity(stock.getAvailableQuantity() + stock.getReservedQuantity())
                .minimumStock(stock.getMinimumStock())
                .maximumStock(stock.getMaximumStock())
                .reorderPoint(stock.getReorderPoint())
                .warehouseLocation(stock.getWarehouseLocation())
                .isLowStock(isLowStock)
                .needsReorder(needsReorder)
                .lastUpdated(stock.getLastUpdated())
                .build();
    }
}
