package com.toolcnc.api.controller;

import com.toolcnc.api.dto.ProductSummaryDTO;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.repository.CategoryRepository;
import com.toolcnc.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<Page<ProductSummaryDTO>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) java.util.List<String> brand,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "false") boolean onlyInStock) {

        Sort sortObj;
        if ("price,asc".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price,desc".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "price");
        } else {
            sortObj = Sort.by(Sort.Direction.DESC, "id");
        }

        // Resolve category name → danh sách ID (cha + tất cả con cháu đệ quy)
        List<Long> categoryIds = Collections.emptyList();
        boolean hasCategories = false;
        if (category != null && !category.isBlank()) {
            var rootOpt = categoryRepository.findByNameIgnoreCase(category);
            if (rootOpt.isPresent()) {
                List<Long> ids = new ArrayList<>();
                collectDescendantIds(rootOpt.get().getId(), ids);
                categoryIds = ids;
                hasCategories = true;
            }
        }

        // Resolve brands
        List<String> brandList = Collections.emptyList();
        boolean hasBrands = false;
        if (brand != null && !brand.isEmpty()) {
            brandList = brand.stream().map(String::toLowerCase).toList();
            hasBrands = true;
        }

        Pageable paging;
        if (size > 0) {
            paging = PageRequest.of(page, size, sortObj);
        } else {
            // Nếu size <= 0, lấy toàn bộ sản phẩm (không phân trang)
            // Lưu ý: Spring Data Pageable không hỗ trợ trực tiếp "lấy tất cả" trong PageRequest
            // Chúng ta sử dụng Integer.MAX_VALUE để mô phỏng việc lấy tất cả
            paging = PageRequest.of(0, Integer.MAX_VALUE, sortObj);
        }

        Page<Product> pageTuts = productRepository.findWithFilters(
                keyword, categoryIds, hasCategories, minPrice, maxPrice, brandList, hasBrands, onlyInStock, true, paging);

        Page<ProductSummaryDTO> dtoPage = pageTuts.map(this::convertToSummaryDTO);

        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Đệ quy thu thập ID của category hiện tại và tất cả category con cháu.
     */
    private void collectDescendantIds(Long parentId, List<Long> result) {
        result.add(parentId);
        List<Long> childIds = categoryRepository.findChildIdsByParentId(parentId);
        for (Long childId : childIds) {
            collectDescendantIds(childId, result);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ProductSummaryDTO convertToSummaryDTO(Product p) {
        return ProductSummaryDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .price(p.getPrice())
                .oldPrice(p.getOldPrice())
                .minPrice(p.getMinPrice())
                .maxPrice(p.getMaxPrice())
                .hasVariants(p.getHasVariants())
                .imageUrl(p.getImageUrl())
                .totalStock(p.getTotalStock())
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                .isActive(p.getIsActive())
                .build();
    }
}
