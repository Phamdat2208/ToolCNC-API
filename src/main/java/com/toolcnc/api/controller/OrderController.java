package com.toolcnc.api.controller;

import com.toolcnc.api.dto.OrderRequest;
import com.toolcnc.api.model.Order;
import com.toolcnc.api.model.OrderItem;
import com.toolcnc.api.model.Product;
import com.toolcnc.api.model.User;
import com.toolcnc.api.repository.OrderRepository;
import com.toolcnc.api.repository.ProductRepository;
import com.toolcnc.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @PostMapping("/checkout")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("User not found");
        }
        
        User user = userOpt.get();
        String trackingNum = "TCNC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Order newOrder = Order.builder()
                .user(user)
                .orderTrackingNumber(trackingNum)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .city(request.getCity())
                .address(request.getAddress())
                .paymentMethod(request.getPaymentMethod())
                .status("PENDING")
                .dateCreated(LocalDateTime.now())
                .lastUpdated(LocalDateTime.now())
                .build();
                
        int totalQty = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;
        
        if (request.getItems() != null) {
            for (OrderRequest.OrderItemDto itemDto : request.getItems()) {
                OrderItem item = OrderItem.builder()
                        .productId(itemDto.getProductId())
                        .quantity(itemDto.getQuantity())
                        .unitPrice(BigDecimal.valueOf(itemDto.getUnitPrice()))
                        .build();
                        
                newOrder.add(item);
                totalQty += itemDto.getQuantity();
                totalPrice = totalPrice.add(BigDecimal.valueOf(itemDto.getUnitPrice() * itemDto.getQuantity()));
            }
        }
        
        newOrder.setTotalQuantity(totalQty);
        newOrder.setTotalPrice(totalPrice);
        
        Order savedOrder = orderRepository.save(newOrder);

        return ResponseEntity.ok(Map.of(
            "message", "Order placed successfully",
            "trackingNumber", savedOrder.getOrderTrackingNumber(),
            "status", savedOrder.getStatus()
        ));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        List<Order> orders = orderRepository.findByUserIdOrderByDateCreatedDesc(userOpt.get().getId());

        List<Map<String, Object>> result = orders.stream().map(order -> {
            List<OrderItem> orderItemsList = order.getOrderItems() != null ? order.getOrderItems() : List.of();
            
            List<Map<String, Object>> items = orderItemsList.stream().map(item -> {
                Product product = item.getProductId() != null ? productRepository.findById(item.getProductId()).orElse(null) : null;
                String productName = product != null && product.getName() != null ? product.getName() : "Sản phẩm không xác định";
                String imageUrl = product != null && product.getImageUrl() != null ? product.getImageUrl() : "";

                Map<String, Object> itemMap = new java.util.HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", productName);
                itemMap.put("imageUrl", imageUrl);
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());
                return itemMap;
            }).collect(Collectors.toList());

            Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("trackingNumber", order.getOrderTrackingNumber());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("totalQuantity", order.getTotalQuantity());
            orderMap.put("status", order.getStatus());
            orderMap.put("dateCreated", order.getDateCreated());
            orderMap.put("items", items);
            return orderMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
