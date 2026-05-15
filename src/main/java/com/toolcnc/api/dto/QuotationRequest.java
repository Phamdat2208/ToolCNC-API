package com.toolcnc.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationRequest {

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String customerName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String customerEmail;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String customerPhone;

    private String companyName;

    @NotNull(message = "ID sản phẩm không được để trống")
    private Long productId;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String productName;

    private Long variantId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    private Integer quantity;

    private String note;
}
