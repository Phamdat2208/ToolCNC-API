package com.toolcnc.api.controller;

import com.toolcnc.api.dto.CategoryRequest;
import com.toolcnc.api.model.Category;
import com.toolcnc.api.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class AdminCategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest req) {
        Category category = Category.builder()
                .name(req.getName())
                .build();
        
        if (req.getParentId() != null) {
            categoryRepository.findById(req.getParentId()).ifPresent(category::setParent);
        }
        
        Category saved = categoryRepository.save(category);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest req) {
        return categoryRepository.findById(id).map(category -> {
            category.setName(req.getName());
            
            if (req.getParentId() != null) {
                if (req.getParentId().equals(id)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Cannot set category as its own parent"));
                }
                categoryRepository.findById(req.getParentId()).ifPresent(category::setParent);
            } else {
                category.setParent(null);
            }
            
            Category updated = categoryRepository.save(category);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        return categoryRepository.findById(id).map(category -> {
            try {
                categoryRepository.delete(category);
                return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete category with products attached"));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
