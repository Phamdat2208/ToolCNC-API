package com.toolcnc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantRequest {
    private Long id;
    private String sku;
    private String variantName;
    private BigDecimal price;
    private Integer stock;
}
