package com.enterprise.shop.service;

import com.enterprise.shop.dto.ShopDto;
import com.enterprise.shop.entity.Shop;
import com.enterprise.shop.exception.DuplicateResourceException;
import com.enterprise.shop.exception.ResourceNotFoundException;
import com.enterprise.shop.logging.CorrelationIdManager;
import com.enterprise.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for Shop management operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final CorrelationIdManager correlationIdManager;

    /**
     * Create a new shop.
     */
    @Transactional
    public ShopDto.Response createShop(ShopDto.CreateRequest request) {
        String traceId = correlationIdManager.getTraceId();
        log.info("[{}] Creating new shop with code: {}", traceId, request.getShopCode());

        // Check for duplicate shop code
        if (shopRepository.existsByShopCode(request.getShopCode())) {
            log.warn("[{}] Duplicate shop code: {}", traceId, request.getShopCode());
            throw new DuplicateResourceException("Shop", "shopCode", request.getShopCode());
        }

        // Build shop entity
        Shop shop = Shop.builder()
                .shopCode(request.getShopCode())
                .name(request.getName())
                .description(request.getDescription())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null ? request.getCountry() : "USA")
                .phone(request.getPhone())
                .email(request.getEmail())
                .status(Shop.ShopStatus.ACTIVE)
                .openingHours(request.getOpeningHours())
                .metadata(request.getMetadata())
                .build();

        Shop savedShop = shopRepository.save(shop);
        log.info("[{}] Shop created successfully with ID: {}", traceId, savedShop.getId());

        return mapToResponse(savedShop);
    }

    /**
     * Update shop details.
     */
    @Transactional
    public ShopDto.Response updateShop(UUID shopId, ShopDto.UpdateRequest request) {
        String traceId = correlationIdManager.getTraceId();
        log.info("[{}] Updating shop with ID: {}", traceId, shopId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));

        // Update fields if provided
        if (request.getName() != null) {
            shop.setName(request.getName());
        }
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getAddressLine1() != null) {
            shop.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            shop.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            shop.setCity(request.getCity());
        }
        if (request.getState() != null) {
            shop.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            shop.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            shop.setCountry(request.getCountry());
        }
        if (request.getPhone() != null) {
            shop.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            shop.setEmail(request.getEmail());
        }
        if (request.getStatus() != null) {
            shop.setStatus(request.getStatus());
        }
        if (request.getOpeningHours() != null) {
            shop.setOpeningHours(request.getOpeningHours());
        }
        if (request.getMetadata() != null) {
            shop.setMetadata(request.getMetadata());
        }

        Shop updatedShop = shopRepository.save(shop);
        log.info("[{}] Shop updated successfully: {}", traceId, shopId);

        return mapToResponse(updatedShop);
    }

    /**
     * Get shop by ID.
     */
    @Transactional(readOnly = true)
    public ShopDto.Response getShopById(UUID shopId) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching shop with ID: {}", traceId, shopId);

        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));

        return mapToResponse(shop);
    }

    /**
     * Get shop by shop code.
     */
    @Transactional(readOnly = true)
    public ShopDto.Response getShopByCode(String shopCode) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching shop with code: {}", traceId, shopCode);

        Shop shop = shopRepository.findByShopCode(shopCode)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopCode));

        return mapToResponse(shop);
    }

    /**
     * Get all shops with pagination.
     */
    @Transactional(readOnly = true)
    public Page<ShopDto.Summary> getAllShops(Pageable pageable) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching all shops - page: {}, size: {}", 
                traceId, pageable.getPageNumber(), pageable.getPageSize());

        return shopRepository.findAll(pageable).map(this::mapToSummary);
    }

    /**
     * Get shops by status.
     */
    @Transactional(readOnly = true)
    public Page<ShopDto.Summary> getShopsByStatus(Shop.ShopStatus status, Pageable pageable) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Fetching shops by status: {}", traceId, status);

        return shopRepository.findByStatus(status, pageable).map(this::mapToSummary);
    }

    /**
     * Search shops by name.
     */
    @Transactional(readOnly = true)
    public Page<ShopDto.Summary> searchShops(String name, Pageable pageable) {
        String traceId = correlationIdManager.getTraceId();
        log.debug("[{}] Searching shops by name: {}", traceId, name);

        return shopRepository.searchByName(name, pageable).map(this::mapToSummary);
    }

    /**
     * Get shop entity by ID (internal use).
     */
    @Transactional(readOnly = true)
    public Shop getShopEntityById(UUID shopId) {
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", shopId));
    }

    /**
     * Map Shop entity to Response DTO.
     */
    private ShopDto.Response mapToResponse(Shop shop) {
        return ShopDto.Response.builder()
                .id(shop.getId())
                .shopCode(shop.getShopCode())
                .name(shop.getName())
                .description(shop.getDescription())
                .addressLine1(shop.getAddressLine1())
                .addressLine2(shop.getAddressLine2())
                .city(shop.getCity())
                .state(shop.getState())
                .postalCode(shop.getPostalCode())
                .country(shop.getCountry())
                .phone(shop.getPhone())
                .email(shop.getEmail())
                .status(shop.getStatus())
                .openingHours(shop.getOpeningHours())
                .metadata(shop.getMetadata())
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .build();
    }

    /**
     * Map Shop entity to Summary DTO.
     */
    private ShopDto.Summary mapToSummary(Shop shop) {
        return ShopDto.Summary.builder()
                .id(shop.getId())
                .shopCode(shop.getShopCode())
                .name(shop.getName())
                .city(shop.getCity())
                .state(shop.getState())
                .status(shop.getStatus())
                .build();
    }
}
