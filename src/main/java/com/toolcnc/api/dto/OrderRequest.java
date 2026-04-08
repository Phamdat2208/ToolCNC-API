package com.toolcnc.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String fullName;
    private String phone;
    private Integer provinceCode;
    private String provinceName;
    private Integer wardCode;
    private String wardName;
    private String address;
    private String paymentMethod;
    private List<OrderItemDto> items;
    
    @Data
    public static class OrderItemDto {
        private Long productId;
        private Long variantId;
        private int quantity;
        private double unitPrice;
    }
}
