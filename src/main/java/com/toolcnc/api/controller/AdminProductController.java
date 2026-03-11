package com.toolcnc.api.controller;

import com.toolcnc.api.dto.ProductRequest;
import com.toolcnc.api.model.Category;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.repository.CategoryRepository;
import com.toolcnc.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        Product product = mapToProduct(request);
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createProducts(@RequestBody List<ProductRequest> requests) {
        List<Product> products = requests.stream().map(this::mapToProduct).collect(Collectors.toList());
        return ResponseEntity.ok(productRepository.saveAll(products));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest req) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product existingProduct = optionalProduct.get();
        existingProduct.setName(req.getName());
        if (req.getSku() != null && !req.getSku().isEmpty()) {
            existingProduct.setSku(req.getSku());
        }
        existingProduct.setPrice(req.getPrice());
        existingProduct.setOldPrice(req.getOldPrice());
        existingProduct.setBrand(req.getBrand());
        existingProduct.setDescription(req.getDescription());
        if (req.getStock() != null) {
            existingProduct.setStock(req.getStock());
        }
        existingProduct.setImageUrl(req.getImageUrl());

        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(existingProduct::setCategory);
        }

        return ResponseEntity.ok(productRepository.save(existingProduct));
    }

    private Product mapToProduct(ProductRequest req) {
        // Fallback or find category
        Long catId = req.getCategoryId() != null ? req.getCategoryId() : 1L;
        Category category = categoryRepository.findById(catId).orElseGet(() -> {
            // Find first or create default
            Optional<Category> first = categoryRepository.findAll().stream().findFirst();
            if(first.isPresent()) return first.get();
            
            Category newCat = Category.builder().name("General Tools").description("Default category").build();
            return categoryRepository.save(newCat);
        });

        String sku = req.getSku();
        if (sku == null || sku.isEmpty()) {
            sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        return Product.builder()
                .name(req.getName())
                .sku(sku)
                .price(req.getPrice())
                .oldPrice(req.getOldPrice())
                .brand(req.getBrand())
                .description(req.getDescription())
                .stock(req.getStock() != null ? req.getStock() : 10)
                .imageUrl(req.getImageUrl())
                .category(category)
                .build();
    }
}
