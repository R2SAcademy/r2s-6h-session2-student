package com.r2s.masterclass;

import com.r2s.masterclass.domain.Order;
import com.r2s.masterclass.domain.OrderItem;
import com.r2s.masterclass.domain.OrderStatus;
import com.r2s.masterclass.domain.Product;
import com.r2s.masterclass.repository.OrderRepository;
import com.r2s.masterclass.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@Slf4j
public class R2SApplication {

    public static void main(String[] args) {
        SpringApplication.run(R2SApplication.class, args);
    }

    /**
     * Seeds initial database records for demonstration:
     * - Flash sale product with 10 units
     * - 50 test orders with 3 items each to visualize the N+1 query problem instantly
     */
    @Bean
    CommandLineRunner initDatabase(ProductRepository productRepository, OrderRepository orderRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                log.info("Database already seeded. Skipping initialization.");
                return;
            }

            log.info("Seeding initial demo data for Session 2 Lab...");

            // 1. Seed Flash Sale Product (Stock = 10 for Concurrency Lab)
            Product flashSalePhone = productRepository.save(Product.builder()
                    .name("Flash Sale iPhone 15 Pro")
                    .sku("IP15P-TITANIUM")
                    .price(BigDecimal.valueOf(25000000))
                    .stockQuantity(10)
                    .build());

            Product macbook = productRepository.save(Product.builder()
                    .name("MacBook Pro M3")
                    .sku("MBP-M3-16GB")
                    .price(BigDecimal.valueOf(45000000))
                    .stockQuantity(50)
                    .build());

            Product airpods = productRepository.save(Product.builder()
                    .name("AirPods Pro 2")
                    .sku("AIRPODS-PRO-2")
                    .price(BigDecimal.valueOf(5500000))
                    .stockQuantity(100)
                    .build());

            // 2. Seed 50 Orders with 3 items each to trigger N+1 query storm
            List<Order> seedOrders = new ArrayList<>();
            for (int i = 1; i <= 50; i++) {
                Order order = Order.builder()
                        .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                        .customerEmail("customer" + i + "@r2s.edu.vn")
                        .status(OrderStatus.PAID)
                        .totalAmount(BigDecimal.valueOf(30500000))
                        .createdAt(LocalDateTime.now().minusHours(i))
                        .items(new ArrayList<>())
                        .build();

                OrderItem item1 = OrderItem.builder()
                        .order(order)
                        .product(flashSalePhone)
                        .quantity(1)
                        .unitPrice(flashSalePhone.getPrice())
                        .build();

                OrderItem item2 = OrderItem.builder()
                        .order(order)
                        .product(airpods)
                        .quantity(1)
                        .unitPrice(airpods.getPrice())
                        .build();

                order.getItems().add(item1);
                order.getItems().add(item2);
                seedOrders.add(order);
            }

            orderRepository.saveAll(seedOrders);
            log.info("Database seeding complete: 3 Products and {} Orders created.", seedOrders.size());
        };
    }
}
