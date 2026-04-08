package com.toolcnc.api.controller;

import com.toolcnc.api.dto.OrderRequest;
import com.toolcnc.api.model.*;
import com.toolcnc.api.repository.OrderRepository;
import com.toolcnc.api.repository.ProductRepository;
import com.toolcnc.api.repository.ProductVariantRepository;
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

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @PostMapping("/checkout")
    @org.springframework.transaction.annotation.Transactional
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
                .provinceCode(request.getProvinceCode())
                .provinceName(request.getProvinceName())
                .wardCode(request.getWardCode())
                .wardName(request.getWardName())
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
                Product product = productRepository.findById(itemDto.getProductId()).orElse(null);
                if (product == null) {
                    return ResponseEntity.status(400).body(Map.of("message", "Sản phẩm không tồn tại trong hệ thống."));
                }
                
                // Stock logic for variants vs simple products
                if (itemDto.getVariantId() != null) {
                    ProductVariant variant = productVariantRepository.findById(itemDto.getVariantId()).orElse(null);
                    if (variant == null) {
                        return ResponseEntity.status(400).body(Map.of("message", "Phiên bản sản phẩm không tồn tại."));
                    }
                    if (variant.getStock() < itemDto.getQuantity()) {
                        return ResponseEntity.status(400).body(Map.of("message", "Phiên bản '" + variant.getVariantName() + "' không đủ hàng. Còn lại: " + variant.getStock()));
                    }
                    // Deduct variant stock
                    variant.setStock(variant.getStock() - itemDto.getQuantity());
                    productVariantRepository.save(variant);
                    
                    // Sync total stock on product
                    Integer totalStock = product.getTotalStock();
                    if (totalStock != null) {
                        product.setTotalStock(totalStock - itemDto.getQuantity());
                        productRepository.save(product);
                    }
                } else {
                    int currentStock = product.getTotalStock() != null ? product.getTotalStock() : 10;
                    if (currentStock < itemDto.getQuantity()) {
                        return ResponseEntity.status(400).body(Map.of("message", "Sản phẩm '" + product.getName() + "' không đủ hàng. Còn lại: " + currentStock));
                    }
                    // Deduct product stock
                    product.setTotalStock(currentStock - itemDto.getQuantity());
                    productRepository.save(product);
                }

                OrderItem item = OrderItem.builder()
                        .productId(itemDto.getProductId())
                        .variantId(itemDto.getVariantId())
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

        java.util.Set<Long> productIds = orders.stream()
                .flatMap(order -> order.getOrderItems() != null ? order.getOrderItems().stream() : java.util.stream.Stream.empty())
                .map(OrderItem::getProductId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
                
        java.util.Map<Long, Product> productMap = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            productRepository.findAllById(productIds).forEach(p -> productMap.put(p.getId(), p));
        }

        java.util.Set<Long> variantIds = orders.stream()
                .flatMap(order -> order.getOrderItems() != null ? order.getOrderItems().stream() : java.util.stream.Stream.empty())
                .map(OrderItem::getVariantId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        java.util.Map<Long, ProductVariant> variantMap = new java.util.HashMap<>();
        if (!variantIds.isEmpty()) {
            productVariantRepository.findAllById(variantIds).forEach(v -> variantMap.put(v.getId(), v));
        }

        List<Map<String, Object>> result = orders.stream().map(order -> {
            List<OrderItem> orderItemsList = order.getOrderItems() != null ? order.getOrderItems() : List.of();
            
            List<Map<String, Object>> items = orderItemsList.stream().map(item -> {
                Product product = item.getProductId() != null ? productMap.get(item.getProductId()) : null;
                String productName = product != null && product.getName() != null ? product.getName() : "Sản phẩm không xác định";
                String imageUrl = product != null && product.getImageUrl() != null ? product.getImageUrl() : "";

                Map<String, Object> itemMap = new java.util.HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", productName);
                itemMap.put("imageUrl", imageUrl);
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());

                // Add variant info from bulk map
                if (item.getVariantId() != null) {
                    itemMap.put("variantId", item.getVariantId());
                    ProductVariant v = variantMap.get(item.getVariantId());
                    if (v != null) {
                        itemMap.put("variantName", v.getVariantName());
                        itemMap.put("sku", v.getSku());
                    }
                } else {
                    // Fallback to product SKU if no variant
                    if (product != null) {
                        itemMap.put("sku", product.getSku());
                    }
                }

                return itemMap;
            }).collect(Collectors.toList());

            Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("trackingNumber", order.getOrderTrackingNumber());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("totalQuantity", order.getTotalQuantity());
            orderMap.put("status", order.getStatus());
            orderMap.put("cancelReason", order.getCancelReason());
            orderMap.put("dateCreated", order.getDateCreated());
            orderMap.put("items", items);
            return orderMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty() || !userOpt.get().getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        List<Order> orders = orderRepository.findAllByOrderByDateCreatedDesc();

        java.util.Set<Long> productIds = orders.stream()
                .flatMap(order -> order.getOrderItems() != null ? order.getOrderItems().stream() : java.util.stream.Stream.empty())
                .map(OrderItem::getProductId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
                
        java.util.Map<Long, Product> productMap = new java.util.HashMap<>();
        if (!productIds.isEmpty()) {
            productRepository.findAllById(productIds).forEach(p -> productMap.put(p.getId(), p));
        }

        java.util.Set<Long> variantIds = orders.stream()
                .flatMap(order -> order.getOrderItems() != null ? order.getOrderItems().stream() : java.util.stream.Stream.empty())
                .map(OrderItem::getVariantId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        java.util.Map<Long, ProductVariant> variantMap = new java.util.HashMap<>();
        if (!variantIds.isEmpty()) {
            productVariantRepository.findAllById(variantIds).forEach(v -> variantMap.put(v.getId(), v));
        }

        List<Map<String, Object>> result = orders.stream().map(order -> {
            List<OrderItem> orderItemsList = order.getOrderItems() != null ? order.getOrderItems() : List.of();
            
            List<Map<String, Object>> items = orderItemsList.stream().map(item -> {
                Product product = item.getProductId() != null ? productMap.get(item.getProductId()) : null;
                String productName = product != null && product.getName() != null ? product.getName() : "Sản phẩm không xác định";
                String imageUrl = product != null && product.getImageUrl() != null ? product.getImageUrl() : "";

                Map<String, Object> itemMap = new java.util.HashMap<>();
                itemMap.put("productId", item.getProductId());
                itemMap.put("productName", productName);
                itemMap.put("imageUrl", imageUrl);
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());

                // Add variant info from bulk map
                if (item.getVariantId() != null) {
                    itemMap.put("variantId", item.getVariantId());
                    ProductVariant v = variantMap.get(item.getVariantId());
                    if (v != null) {
                        itemMap.put("variantName", v.getVariantName());
                        itemMap.put("sku", v.getSku());
                    }
                } else {
                    // Fallback to product SKU if no variant
                    if (product != null) {
                        itemMap.put("sku", product.getSku());
                    }
                }

                return itemMap;
            }).collect(Collectors.toList());

            Map<String, Object> orderMap = new java.util.HashMap<>();
            orderMap.put("id", order.getId());
            orderMap.put("trackingNumber", order.getOrderTrackingNumber());
            orderMap.put("customerName", order.getFullName());
            orderMap.put("phone", order.getPhone());
            orderMap.put("address", order.getAddress());
            orderMap.put("wardName", order.getWardName());
            orderMap.put("provinceName", order.getProvinceName());
            orderMap.put("paymentMethod", order.getPaymentMethod());
            orderMap.put("totalPrice", order.getTotalPrice());
            orderMap.put("totalQuantity", order.getTotalQuantity());
            orderMap.put("status", order.getStatus());
            orderMap.put("cancelReason", order.getCancelReason());
            orderMap.put("dateCreated", order.getDateCreated());
            orderMap.put("items", items);
            return orderMap;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/status")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty() || !userOpt.get().getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }
        
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Order order = orderOpt.get();
        if(payload.containsKey("status")) {
            String newStatus = payload.get("status").toUpperCase();
            String oldStatus = order.getStatus();

            // Nếu admin cập nhật thành CANCELLED thì hoàn tồn kho
            if ("CANCELLED".equals(newStatus) && !"CANCELLED".equals(oldStatus)) {
                if (payload.containsKey("cancelReason")) {
                    order.setCancelReason(payload.get("cancelReason"));
                }
                
                if (order.getOrderItems() != null) {
                    for (OrderItem item : order.getOrderItems()) {
                        // Revert variant stock if exists
                        if (item.getVariantId() != null) {
                            productVariantRepository.findById(item.getVariantId()).ifPresent(variant -> {
                                variant.setStock(variant.getStock() + item.getQuantity());
                                productVariantRepository.save(variant);
                                
                                // Sync total stock
                                productRepository.findById(item.getProductId()).ifPresent(product -> {
                                    product.setTotalStock((product.getTotalStock() != null ? product.getTotalStock() : 0) + item.getQuantity());
                                    productRepository.save(product);
                                });
                            });
                        } else {
                            // Revert product stock only
                            productRepository.findById(item.getProductId()).ifPresent(product -> {
                                product.setTotalStock((product.getTotalStock() != null ? product.getTotalStock() : 0) + item.getQuantity());
                                productRepository.save(product);
                            });
                        }
                    }
                }
            }

            order.setStatus(newStatus);
            order.setLastUpdated(LocalDateTime.now());
            orderRepository.save(order);
            return ResponseEntity.ok(Map.of("message", "Order status updated successfully", "newStatus", order.getStatus()));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Status is required"));
    }

    @PutMapping("/{id}/cancel")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        Optional<User> userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "User not found"));
        }

        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        if (!order.getUser().getId().equals(userOpt.get().getId())) {
             return ResponseEntity.status(403).body(Map.of("message", "Bạn không có quyền thao tác trên đơn hàng này"));
        }

        if (!"PENDING".equals(order.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Bạn chỉ có thể hủy những đơn hàng đang chờ xử lý. Vui lòng liên hệ quản trị viên."));
        }

        // Hoàn lại kho
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                if (item.getVariantId() != null) {
                    productVariantRepository.findById(item.getVariantId()).ifPresent(variant -> {
                        variant.setStock(variant.getStock() + item.getQuantity());
                        productVariantRepository.save(variant);
                        
                        // Sync total stock
                        productRepository.findById(item.getProductId()).ifPresent(product -> {
                            product.setTotalStock((product.getTotalStock() != null ? product.getTotalStock() : 0) + item.getQuantity());
                            productRepository.save(product);
                        });
                    });
                } else {
                    productRepository.findById(item.getProductId()).ifPresent(product -> {
                        product.setTotalStock((product.getTotalStock() != null ? product.getTotalStock() : 0) + item.getQuantity());
                        productRepository.save(product);
                    });
                }
            }
        }

        order.setStatus("CANCELLED");
        order.setLastUpdated(LocalDateTime.now());
        if (payload.containsKey("cancelReason")) {
            order.setCancelReason(payload.get("cancelReason"));
        }
        orderRepository.save(order);

        return ResponseEntity.ok(Map.of("message", "Đã hủy đơn hàng thành công", "newStatus", "CANCELLED"));
    }

    @GetMapping("/track/{trackingNumber}")
    public ResponseEntity<?> trackOrder(@PathVariable String trackingNumber) {
        Optional<Order> orderOpt = orderRepository.findByOrderTrackingNumber(trackingNumber);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy đơn hàng với mã tra cứu: " + trackingNumber));
        }

        Order order = orderOpt.get();
        List<OrderItem> orderItemsList = order.getOrderItems() != null ? order.getOrderItems() : List.of();

        java.util.Set<Long> variantIds = orderItemsList.stream()
                .map(OrderItem::getVariantId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        java.util.Map<Long, ProductVariant> variantMap = new java.util.HashMap<>();
        if (!variantIds.isEmpty()) {
            productVariantRepository.findAllById(variantIds).forEach(v -> variantMap.put(v.getId(), v));
        }

        List<Map<String, Object>> items = orderItemsList.stream().map(item -> {
            Product product = item.getProductId() != null ? productRepository.findById(item.getProductId()).orElse(null) : null;
            String productName = product != null && product.getName() != null ? product.getName() : "Sản phẩm không xác định";
            String imageUrl = product != null && product.getImageUrl() != null ? product.getImageUrl() : "";

            Map<String, Object> itemMap = new java.util.HashMap<>();
            itemMap.put("productName", productName);
            itemMap.put("imageUrl", imageUrl);
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("unitPrice", item.getUnitPrice());
            
            if (item.getVariantId() != null) {
                ProductVariant v = variantMap.get(item.getVariantId());
                if (v != null) {
                    itemMap.put("variantName", v.getVariantName());
                    itemMap.put("sku", v.getSku());
                }
            } else {
                if (product != null) {
                    itemMap.put("sku", product.getSku());
                }
            }
            
            return itemMap;
        }).collect(Collectors.toList());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("trackingNumber", order.getOrderTrackingNumber());
        result.put("fullName", order.getFullName());
        result.put("phone", order.getPhone());
        String fullAddress = order.getAddress() + ", " + order.getWardName() + ", " + order.getProvinceName();
        result.put("address", fullAddress);
        result.put("paymentMethod", order.getPaymentMethod());
        result.put("totalPrice", order.getTotalPrice());
        result.put("totalQuantity", order.getTotalQuantity());
        result.put("status", order.getStatus());
        result.put("cancelReason", order.getCancelReason());
        result.put("dateCreated", order.getDateCreated());
        result.put("lastUpdated", order.getLastUpdated());
        result.put("items", items);

        return ResponseEntity.ok(result);
    }
}
