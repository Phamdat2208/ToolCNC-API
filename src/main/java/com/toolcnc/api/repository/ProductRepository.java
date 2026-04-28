package com.toolcnc.api.repository;

import com.toolcnc.api.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryIdAndIsActiveTrue(Long categoryId, Pageable pageable);
    Page<Product> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);
    Page<Product> findByIsActive(Boolean isActive, Pageable pageable);
    Page<Product> findByIsActiveTrue(Pageable pageable);
    boolean existsByName(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);

    @Query("SELECT p.name FROM Product p WHERE LOWER(p.name) IN :names")
    java.util.List<String> findExistingNames(@Param("names") java.util.List<String> lowerCaseNames);

    // Fetch products with brand in a single JOIN — avoids N+1 from EAGER brand loading
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.brand WHERE p.id IN :ids")
    java.util.List<Product> findByIdInWithBrand(@Param("ids") java.util.Set<Long> ids);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category", "brand"})
    @Query("SELECT p FROM Product p WHERE " +
           "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:hasCategories = false OR p.category.id IN :categoryIds) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:hasBrands = false OR LOWER(p.brand.name) IN :brands) AND " +
           "(:onlyInStock = false OR p.totalStock > 0) AND " +
           "(:isActive IS NULL OR p.isActive = :isActive)")
    Page<Product> findWithFilters(
        @Param("keyword") String keyword,
        @Param("categoryIds") java.util.List<Long> categoryIds,
        @Param("hasCategories") boolean hasCategories,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("brands") java.util.List<String> brands,
        @Param("hasBrands") boolean hasBrands,
        @Param("onlyInStock") boolean onlyInStock,
        @Param("isActive") Boolean isActive,
        Pageable pageable
    );
}
