package com.toolcnc.api.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal oldPrice;
    private String brand;
    private String description;
    private Integer stock;
    private String imageUrl;
    private String specifications;
    private Long categoryId;
}
