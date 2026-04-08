package com.toolcnc.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse implements java.io.Serializable {
    private Long id;
    private String name;
    private Long parentId;
    private Long productCount;
    private java.util.List<CategoryResponse> children;

    public CategoryResponse(Long id, String name, Long parentId, Long productCount) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.productCount = productCount;
    }
}
