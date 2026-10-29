package com.r2s.masterclass.repository;

import com.r2s.masterclass.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);

    /**
     * AI-GENERATED BUGGY QUERY:
     * AI viết câu lệnh UPDATE trừ trực tiếp trong DB để "tối ưu hiệu năng",
     * nhưng QUÊN THIẾU điều kiện kiểm tra tồn kho "AND p.stockQuantity >= :quantity".
     * Kết hợp với việc kiểm tra trên RAM không có Lock, câu lệnh này sẽ làm âm kho hàng!
     */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity WHERE p.id = :id")
    int deductStockDirect(@Param("id") Long id, @Param("quantity") Integer quantity);
}
