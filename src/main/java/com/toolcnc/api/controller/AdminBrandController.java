package com.toolcnc.api.controller;

import com.toolcnc.api.dto.BrandRequest;
import com.toolcnc.api.model.Brand;
import com.toolcnc.api.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/brands")
@RequiredArgsConstructor
@CrossOrigin
@PreAuthorize("hasRole('ADMIN')")
public class AdminBrandController {

    private final BrandRepository brandRepository;

    @GetMapping
    public ResponseEntity<Page<Brand>> getAllBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(brandRepository.findAll(pageable));
    }

    @PostMapping
    @CacheEvict(value = "brands", allEntries = true)
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
    @CacheEvict(value = "brands", allEntries = true)
    public ResponseEntity<?> updateBrand(@PathVariable Long id, @Valid @RequestBody BrandRequest request) {
        return brandRepository.findById(id).map(brand -> {
            brand.setName(request.getName());
            brand.setLogoUrl(request.getLogoUrl());
            brand.setDescription(request.getDescription());
            return ResponseEntity.ok(brandRepository.save(brand));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @CacheEvict(value = "brands", allEntries = true)
    public ResponseEntity<?> deleteBrand(@PathVariable Long id) {
        if (brandRepository.existsById(id)) {
            brandRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
