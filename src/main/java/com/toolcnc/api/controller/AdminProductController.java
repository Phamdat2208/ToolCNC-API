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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            return ResponseEntity.badRequest().body("Tên sản phẩm '" + request.getName() + "' đã tồn tại!");
        }
        Product product = mapToProduct(request);
        return ResponseEntity.ok(productRepository.save(product));
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> createProducts(@RequestBody List<ProductRequest> requests) {
        List<String> duplicateNames = requests.stream()
                .map(ProductRequest::getName)
                .filter(name -> productRepository.existsByNameIgnoreCase(name))
                .collect(Collectors.toList());

        if (!duplicateNames.isEmpty()) {
            return ResponseEntity.badRequest().body("Các sản phẩm sau đã tồn tại tên: " + String.join(", ", duplicateNames));
        }

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
        
        // Kiểm tra trùng tên khi đổi tên (không phân biệt hoa thường, trừ chính nó)
        if (productRepository.existsByNameIgnoreCaseAndIdNot(req.getName(), id)) {
            return ResponseEntity.badRequest().body("Tên sản phẩm '" + req.getName() + "' đã tồn tại!");
        }

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
        
        // Optimize local storage by deleting old image when changed
        if (req.getImageUrl() != null && !req.getImageUrl().equals(existingProduct.getImageUrl())) {
            deleteOldImage(existingProduct.getImageUrl());
            existingProduct.setImageUrl(req.getImageUrl());
        } else if (req.getImageUrl() == null && existingProduct.getImageUrl() != null) {
           // If request clears the image entirely
           deleteOldImage(existingProduct.getImageUrl());
           existingProduct.setImageUrl(null);
        }

        existingProduct.setSpecifications(req.getSpecifications());

        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(existingProduct::setCategory);
        }

        return ResponseEntity.ok(productRepository.save(existingProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = optionalProduct.get();
        // Remove locally hosted image file if exists
        deleteOldImage(product.getImageUrl());
        
        productRepository.delete(product);
        return ResponseEntity.ok().build();
    }

    private void deleteOldImage(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("/uploads/products/")) {
            try {
                String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                Path imagePath = Paths.get("uploads/products/", filename);
                Files.deleteIfExists(imagePath);
            } catch (Exception e) {
                System.err.println("Could not delete old image: " + e.getMessage());
            }
        }
    }

    private Product mapToProduct(ProductRequest req) {
        if (req.getCategoryId() == null) {
            throw new RuntimeException("Category ID is required for a new product");
        }
        
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + req.getCategoryId()));

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
                .specifications(req.getSpecifications())
                .category(category)
                .build();
    }
}
