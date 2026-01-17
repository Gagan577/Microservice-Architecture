package com.enterprise.shop.repository;

import com.enterprise.shop.entity.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Shop entity operations.
 */
@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {

    /**
     * Find shop by unique shop code.
     */
    Optional<Shop> findByShopCode(String shopCode);

    /**
     * Check if shop code already exists.
     */
    boolean existsByShopCode(String shopCode);

    /**
     * Find all shops by status.
     */
    Page<Shop> findByStatus(Shop.ShopStatus status, Pageable pageable);

    /**
     * Find shops by city.
     */
    Page<Shop> findByCity(String city, Pageable pageable);

    /**
     * Find shops by status list.
     */
    List<Shop> findByStatusIn(List<Shop.ShopStatus> statuses);

    /**
     * Search shops by name (case-insensitive).
     */
    @Query("SELECT s FROM Shop s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Shop> searchByName(@Param("name") String name, Pageable pageable);

    /**
     * Find active shops by city.
     */
    @Query("SELECT s FROM Shop s WHERE s.city = :city AND s.status = 'ACTIVE'")
    List<Shop> findActiveShopsByCity(@Param("city") String city);

    /**
     * Count shops by status.
     */
    long countByStatus(Shop.ShopStatus status);

    /**
     * Find shops with specific opening hours (for queries involving JSON).
     */
    @Query(value = "SELECT * FROM shop_schema.shops WHERE opening_hours IS NOT NULL AND opening_hours ->> :day IS NOT NULL", nativeQuery = true)
    List<Shop> findShopsOpenOn(@Param("day") String day);

    /**
     * Get shop summary statistics.
     */
    @Query("SELECT s.status, COUNT(s) FROM Shop s GROUP BY s.status")
    List<Object[]> getShopCountByStatus();
}
