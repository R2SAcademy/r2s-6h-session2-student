package com.r2s.masterclass.repository;

import com.r2s.masterclass.domain.Order;
import com.r2s.masterclass.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Default basic query without JOIN FETCH -> causes N+1 queries when accessing items
    List<Order> findByStatus(OrderStatus status);
}
