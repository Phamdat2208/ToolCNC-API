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
        if (request.getSku() != null && !request.getSku().isEmpty() && productRepository.existsBySku(request.getSku())) {
            return ResponseEntity.badRequest().body("Mã SKU đã tồn tại!");
        }
        Product product = mapToProduct(request);
        
        // Handle variants for new product
        if (Boolean.TRUE.equals(request.getHasVariants()) && request.getVariants() != null && !request.getVariants().isEmpty()) {
            request.getVariants().forEach(vReq -> {
                com.toolcnc.api.model.ProductVariant variant = com.toolcnc.api.model.ProductVariant.builder()
                        .sku(generateVariantSku(product.getSku(), vReq.getSku()))
                        .variantName(vReq.getVariantName())
                        .price(vReq.getPrice())
                        .stock(vReq.getStock())
                        .product(product)
                        .build();
                product.getVariants().add(variant);
            });
            calculatePriceRange(product, request.getVariants());
        } else {
            product.setMinPrice(product.getPrice());
            product.setMaxPrice(product.getPrice());
        }

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

        // --- VARIANTS LOGIC ---
        existingProduct.setHasVariants(Boolean.TRUE.equals(req.getHasVariants()));
        if (Boolean.TRUE.equals(req.getHasVariants()) && req.getVariants() != null) {
            // Remove variants not in request
            java.util.Set<Long> reqIds = req.getVariants().stream()
                    .map(com.toolcnc.api.dto.ProductVariantRequest::getId)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toSet());
            existingProduct.getVariants().removeIf(v -> !reqIds.contains(v.getId()));

            // Update or Add
            req.getVariants().forEach(vReq -> {
                if (vReq.getId() != null) {
                    existingProduct.getVariants().stream()
                            .filter(v -> v.getId().equals(vReq.getId()))
                            .findFirst()
                            .ifPresent(v -> {
                                v.setSku(generateVariantSku(existingProduct.getSku(), vReq.getSku()));
                                v.setVariantName(vReq.getVariantName());
                                v.setPrice(vReq.getPrice());
                                v.setStock(vReq.getStock());
                            });
                } else {
                    com.toolcnc.api.model.ProductVariant variant = com.toolcnc.api.model.ProductVariant.builder()
                            .sku(generateVariantSku(existingProduct.getSku(), vReq.getSku()))
                            .variantName(vReq.getVariantName())
                            .price(vReq.getPrice())
                            .stock(vReq.getStock())
                            .product(existingProduct)
                            .build();
                    existingProduct.getVariants().add(variant);
                }
            });
            calculatePriceRange(existingProduct, req.getVariants());
        } else {
            existingProduct.getVariants().clear();
            existingProduct.setMinPrice(existingProduct.getPrice());
            existingProduct.setMaxPrice(existingProduct.getPrice());
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
                .hasVariants(Boolean.TRUE.equals(req.getHasVariants()))
                .price(req.getPrice())
                .oldPrice(req.getOldPrice())
                .description(req.getDescription())
                .stock(req.getStock() != null ? req.getStock() : 0)
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

    private String generateVariantSku(String parentSku, String variantSkuInput) {
        if (variantSkuInput != null && !variantSkuInput.isBlank()) {
            return variantSkuInput.trim();
        }
        return parentSku + "-V-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void calculatePriceRange(Product product, List<com.toolcnc.api.dto.ProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            product.setMinPrice(product.getPrice());
            product.setMaxPrice(product.getPrice());
            return;
        }

        java.math.BigDecimal min = variants.stream()
                .map(com.toolcnc.api.dto.ProductVariantRequest::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(product.getPrice());

        java.math.BigDecimal max = variants.stream()
                .map(com.toolcnc.api.dto.ProductVariantRequest::getPrice)
                .filter(java.util.Objects::nonNull)
                .max(java.math.BigDecimal::compareTo)
                .orElse(product.getPrice());

        product.setMinPrice(min);
        product.setMaxPrice(max);
        
        // Ensure price field is also consistent (usually min price)
        product.setPrice(min);
    }
}
