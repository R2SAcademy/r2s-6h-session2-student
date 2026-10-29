package com.r2s.masterclass.controller;

import com.r2s.masterclass.domain.OrderStatus;
import com.r2s.masterclass.dto.OrderResponseDTO;
import com.r2s.masterclass.repository.OrderRepository;
import com.r2s.masterclass.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders(
            @RequestParam(defaultValue = "PAID") OrderStatus status) {
        return ResponseEntity.ok(orderService.getAllOrdersWithDetails(status));
    }

    @PostMapping("/seed")
    public ResponseEntity<?> seedOrders(@RequestParam(defaultValue = "1000") int count) {
        int seeded = orderService.seedOrders(count);
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Seeded " + seeded + " orders for N+1 SQL storm demo",
                "totalOrdersInDb", orderRepository.count()
        ));
    }
}
