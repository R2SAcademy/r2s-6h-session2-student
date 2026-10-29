package com.r2s.masterclass.service;

import com.r2s.masterclass.domain.Order;
import com.r2s.masterclass.domain.OrderItem;
import com.r2s.masterclass.domain.OrderStatus;
import com.r2s.masterclass.domain.Product;
import com.r2s.masterclass.dto.OrderItemResponseDTO;
import com.r2s.masterclass.dto.OrderResponseDTO;
import com.r2s.masterclass.repository.OrderRepository;
import com.r2s.masterclass.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Helper method to generate bulk orders for N+1 SQL storm demo
     */
    @Transactional
    public int seedOrders(int count) {
        log.info("Generating {} demo orders for N+1 SQL storm demo...", count);
        Product sampleProduct = productRepository.findAll().stream().findFirst()
                .orElseGet(() -> productRepository.save(Product.builder()
                        .name("Demo Bulk Product")
                        .sku("BULK-" + System.currentTimeMillis())
                        .price(BigDecimal.valueOf(100000))
                        .stockQuantity(5000)
                        .build()));

        List<Order> newOrders = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Order order = Order.builder()
                    .orderCode("BULK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .customerEmail("stress" + i + "@r2s.edu.vn")
                    .status(OrderStatus.PAID)
                    .totalAmount(BigDecimal.valueOf(100000))
                    .createdAt(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(sampleProduct)
                    .quantity(1)
                    .unitPrice(sampleProduct.getPrice())
                    .build();

            order.getItems().add(item);
            newOrders.add(order);
        }

        orderRepository.saveAll(newOrders);
        log.info("Successfully seeded {} orders in DB.", newOrders.size());
        return newOrders.size();
    }

    /**
     * AI-GENERATED METHOD:
     * // TODO [Lab Task 1]: Audit SQL logs. Notice 1,000+ SELECT queries generated. Refactor to 1 query using JOIN FETCH.
     *
     * BUG 1 (N+1 Query Defect):
     * - Query 1: SELECT * FROM orders WHERE status = ?
     * - Loop Query N: For every order, order.getItems() triggers: SELECT * FROM order_items WHERE order_id = ?
     * - Loop Query N*M: For every item, item.getProduct().getName() triggers: SELECT * FROM products WHERE id = ?
     * -> If there are 1,000 orders, this generates over 1,001+ round-trips to the database!
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getAllOrdersWithDetails(OrderStatus status) {
        log.info("Fetching orders with status: {}", status);

        // 1. Root query
        List<Order> orders = orderRepository.findByStatus(status);
        List<OrderResponseDTO> results = new ArrayList<>();

        // 2. Fatal In-Memory Iteration causing N+1 SQL storm
        int orderIndex = 0;
        for (Order order : orders) {
            orderIndex++;
            if (orderIndex <= 3 || orderIndex % 250 == 0 || orderIndex == orders.size()) {
                log.warn("[N+1 QUERY DETECTED] Đang kích hoạt câu SQL con lấy Items cho Order #{}/{} (Mã: {})", 
                        orderIndex, orders.size(), order.getOrderCode());
            }

            List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                    .map(item -> OrderItemResponseDTO.builder()
                            .itemId(item.getId())
                            .productId(item.getProduct().getId())
                            .productName(item.getProduct().getName()) // Triggers lazy load of Product!
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .build())
                    .collect(Collectors.toList());

            OrderResponseDTO dto = OrderResponseDTO.builder()
                    .orderId(order.getId())
                    .orderCode(order.getOrderCode())
                    .customerEmail(order.getCustomerEmail())
                    .status(order.getStatus())
                    .totalAmount(order.getTotalAmount())
                    .createdAt(order.getCreatedAt())
                    .items(itemDTOs)
                    .build();

            results.add(dto);
        }

        log.info("Finished processing {} orders into DTOs", results.size());
        return results;
    }
}
