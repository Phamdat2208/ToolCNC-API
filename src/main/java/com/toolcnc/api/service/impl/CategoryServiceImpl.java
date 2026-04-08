package com.toolcnc.api.service.impl;

import com.toolcnc.api.dto.CategoryResponse;
import com.toolcnc.api.repository.CategoryRepository;
import com.toolcnc.api.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Cacheable(value = "categoryTree")
    public List<CategoryResponse> getCategoryTree() {
        List<CategoryResponse> all = categoryRepository.findAllWithProductCount();
        
        Map<Long, CategoryResponse> map = new HashMap<>();
        List<CategoryResponse> roots = new ArrayList<>();

        // Map all items
        for (CategoryResponse item : all) {
            map.put(item.getId(), item);
        }

        // Link parent-child
        for (CategoryResponse item : all) {
            if (item.getParentId() == null) {
                roots.add(item);
            } else {
                CategoryResponse parent = map.get(item.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(item);
                }
            }
        }

        return roots;
    }
}
