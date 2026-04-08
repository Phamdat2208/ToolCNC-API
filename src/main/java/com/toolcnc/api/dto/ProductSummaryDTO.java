package com.toolcnc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO {
    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean hasVariants;
    private String imageUrl;
    private Integer totalStock;
    private String categoryName;
    private String brandName;
}
