package com.toolcnc.api.controller;

import com.toolcnc.api.dto.QuotationRequest;
import com.toolcnc.api.model.Quotation;
import com.toolcnc.api.repository.QuotationRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class QuotationController {

    @Autowired
    private QuotationRepository quotationRepository;

    @Autowired
    private com.toolcnc.api.repository.UserRepository userRepository;

    private com.toolcnc.api.model.User getAuthenticatedUser(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        String username = auth.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    @PostMapping("/public/quotations")
    public ResponseEntity<?> submitQuotation(@Valid @RequestBody QuotationRequest request) {
        Quotation quotation = Quotation.builder()
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .companyName(request.getCompanyName())
                .productId(request.getProductId())
                .productName(request.getProductName())
                .variantId(request.getVariantId())
                .quantity(request.getQuantity())
                .note(request.getNote())
                .status("PENDING")
                .build();

        Quotation saved = quotationRepository.save(quotation);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Yêu cầu báo giá đã được gửi thành công",
                "id", saved.getId()
        ));
    }

    @GetMapping("/quotations/my")
    public ResponseEntity<?> getMyQuotations(org.springframework.security.core.Authentication auth) {
        com.toolcnc.api.model.User user = getAuthenticatedUser(auth);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        
        // Tìm theo email của user hiện tại
        java.util.List<Quotation> myQuotations = quotationRepository.findByCustomerEmailOrderByCreatedAtDesc(user.getEmail());
        return ResponseEntity.ok(myQuotations);
    }

    @GetMapping("/admin/quotations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Quotation>> getAllQuotations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable paging = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(quotationRepository.findAll(paging));
    }

    @PutMapping("/admin/quotations/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> statusRequest) {
        
        String newStatus = statusRequest.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body("Trạng thái không được để trống");
        }

        return quotationRepository.findById(id)
                .map(quotation -> {
                    quotation.setStatus(newStatus);
                    quotationRepository.save(quotation);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật trạng thái thành công"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
