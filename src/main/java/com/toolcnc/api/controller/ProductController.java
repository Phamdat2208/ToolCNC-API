package com.toolcnc.api.controller;

import com.toolcnc.api.model.Product;
import com.toolcnc.api.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/public/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String sort) {

        Sort sortObj = Sort.unsorted();
        if ("price,asc".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.ASC, "price");
        } else if ("price,desc".equals(sort)) {
            sortObj = Sort.by(Sort.Direction.DESC, "price");
        } else {
            sortObj = Sort.by(Sort.Direction.DESC, "id");
        }

        Pageable paging = PageRequest.of(page, size, sortObj);
        Page<Product> pageTuts = productRepository.findWithFilters(keyword, category, minPrice, maxPrice, brand, paging);

        return ResponseEntity.ok(pageTuts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable("id") long id) {
        return productRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
