package com.toolcnc.api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class BrandRequest {

    @NotBlank(message = "Tên thương hiệu không được để trống")
    private String name;

    private String logoUrl;

    private String description;
}
