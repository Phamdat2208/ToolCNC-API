package com.toolcnc.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderRequest {
    private String fullName;
    private String phone;
    private String city;
    private String address;
    private String paymentMethod;
    private List<OrderItemDto> items;
    
    @Data
    public static class OrderItemDto {
        private Long productId;
        private int quantity;
        private double unitPrice;
    }
}
