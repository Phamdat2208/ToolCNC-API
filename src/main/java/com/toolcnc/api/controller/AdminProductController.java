package com.toolcnc.api.controller;

import com.toolcnc.api.dto.ProductRequest;
import com.toolcnc.api.model.Category;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.ProductImage;
import com.toolcnc.api.repository.BrandRepository;
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

    @Autowired
    private BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            return ResponseEntity.badRequest().body("Mã SKU đã tồn tại!");
        }
        Product product = mapToProduct(request);
        Product saved = productRepository.save(product);
        // Save gallery images (max 8)
        applyGallery(saved, request.getImageGallery());
        return ResponseEntity.ok(productRepository.save(saved));
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
        
        existingProduct.setName(req.getName());
        if (req.getSku() != null && !req.getSku().isEmpty()) {
            existingProduct.setSku(req.getSku());
        }
        existingProduct.setPrice(req.getPrice());
        existingProduct.setOldPrice(req.getOldPrice());
        existingProduct.setDescription(req.getDescription());
        
        if (req.getStock() != null) {
            existingProduct.setStock(req.getStock());
        }
        
        // Optimize local storage by deleting old image when changed
        if (req.getImageUrl() != null && !req.getImageUrl().equals(existingProduct.getImageUrl())) {
            deleteOldImage(existingProduct.getImageUrl());
            existingProduct.setImageUrl(req.getImageUrl());
        } else if (req.getImageUrl() == null && existingProduct.getImageUrl() != null) {
           deleteOldImage(existingProduct.getImageUrl());
           existingProduct.setImageUrl(null);
        }

        existingProduct.setSpecifications(req.getSpecifications());

        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(existingProduct::setCategory);
        }

        // --- NEW BRAND LOGIC ---
        if (req.getBrandId() != null) {
            brandRepository.findById(req.getBrandId()).ifPresent(existingProduct::setBrand);
        }

        // Update gallery images (max 8)
        applyGallery(existingProduct, req.getImageGallery());

        return ResponseEntity.ok(productRepository.save(existingProduct));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        Optional<Product> optionalProduct = productRepository.findById(id);
        if (optionalProduct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = optionalProduct.get();
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

    private void applyGallery(Product product, List<String> urls) {
        if (product.getImages() == null) {
            product.setImages(new java.util.ArrayList<>());
        }
        product.getImages().clear();
        if (urls == null || urls.isEmpty()) return;
        int limit = Math.min(urls.size(), 8); // Max 8 images
        for (int i = 0; i < limit; i++) {
            String url = urls.get(i);
            if (url != null && !url.isBlank()) {
                ProductImage img = ProductImage.builder()
                        .url(url.trim())
                        .sortOrder(i)
                        .product(product)
                        .build();
                product.getImages().add(img);
            }
        }
    }

    private Product mapToProduct(ProductRequest req) {
        String sku = req.getSku();
        if (sku == null || sku.isEmpty()) {
            sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        Product.ProductBuilder builder = Product.builder()
                .name(req.getName())
                .sku(sku)
                .price(req.getPrice())
                .oldPrice(req.getOldPrice())
                .description(req.getDescription())
                .stock(req.getStock() != null ? req.getStock() : 10)
                .imageUrl(req.getImageUrl())
                .specifications(req.getSpecifications());

        if (req.getCategoryId() != null) {
            categoryRepository.findById(req.getCategoryId()).ifPresent(builder::category);
        }

        if (req.getBrandId() != null) {
            brandRepository.findById(req.getBrandId()).ifPresent(builder::brand);
        }

        return builder.build();
    }
}
