package com.toolcnc.api.controller;

import com.toolcnc.api.dto.BrandRequest;
import com.toolcnc.api.model.Brand;
import com.toolcnc.api.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@CrossOrigin
public class AdminBrandController {

    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<List<Brand>> getAllBrands() {
        return ResponseEntity.ok(brandRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<?> createBrand(@Valid @RequestBody BrandRequest request) {
        if (brandRepository.findByName(request.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Tên thương hiệu đã tồn tại!");
        }
        Brand brand = Brand.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .build();
        return ResponseEntity.ok(brandRepository.save(brand));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return brandRepository.findById(id).map(brand -> {
            brand.setName(request.getName());
            brand.setLogoUrl(request.getLogoUrl());
            brand.setDescription(request.getDescription());
            return ResponseEntity.ok(brandRepository.save(brand));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        if (brandRepository.existsById(id)) {
            brandRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
