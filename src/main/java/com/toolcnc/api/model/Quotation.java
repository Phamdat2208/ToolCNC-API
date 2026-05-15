package com.toolcnc.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "quotations")
public class Quotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String customerPhone;

    private String companyName;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private String productName;

    private Long variantId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, CONTACTED, DONE, REJECTED

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
