package com.toolcnc.api.service;

import com.toolcnc.api.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getCategoryTree();
}
