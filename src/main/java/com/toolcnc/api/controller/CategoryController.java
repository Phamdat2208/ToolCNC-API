package com.toolcnc.api.controller;

import com.toolcnc.api.dto.CategoryResponse;
import com.toolcnc.api.model.Category;
import com.toolcnc.api.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        List<CategoryResponse> all = categoryRepository.findAllWithProductCount();
        
        // Build hierarchy map
        java.util.Map<Long, CategoryResponse> map = new java.util.HashMap<>();
        for (CategoryResponse res : all) {
            map.put(res.getId(), res);
        }

        // Get actual entities to find parent-child relationships
        List<Category> entities = categoryRepository.findAll();
        List<CategoryResponse> roots = new java.util.ArrayList<>();

        for (Category entity : entities) {
            CategoryResponse current = map.get(entity.getId());
            if (entity.getParent() == null) {
                roots.add(current);
            } else {
                CategoryResponse parent = map.get(entity.getParent().getId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new java.util.ArrayList<>());
                    }
                    parent.getChildren().add(current);
                }
            }
        }

        return ResponseEntity.ok(roots);
    }
}
