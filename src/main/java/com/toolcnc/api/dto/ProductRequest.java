package com.toolcnc.api.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private Long brandId;
    private String description;
    private Integer stock;
    private String imageUrl;
    private List<String> imageGallery; // max 8 images
    private String specifications;
    private Long categoryId;
    private Boolean hasVariants;
    private List<ProductVariantRequest> variants;
}
