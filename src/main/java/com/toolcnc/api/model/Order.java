package com.toolcnc.api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String orderTrackingNumber;

    private BigDecimal totalPrice;

    private Integer totalQuantity;

    private String status; // PENDING, PROCESSING, COMPLETED, CANCELLED

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    private LocalDateTime dateCreated;

    private LocalDateTime lastUpdated;
    
    // Address Info
    private String fullName;
    private String phone;
    private Integer provinceCode;
    private String provinceName;
    private Integer wardCode;
    private String wardName;
    private String address;
    
    private String paymentMethod; // COD, TRANSFER

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "order")
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    public void add(OrderItem item) {
        if (item != null) {
            orderItems.add(item);
            item.setOrder(this);
        }
    }
}
