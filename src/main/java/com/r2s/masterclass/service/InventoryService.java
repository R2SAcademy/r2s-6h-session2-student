package com.r2s.masterclass.service;

import com.r2s.masterclass.domain.Product;
import com.r2s.masterclass.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final ProductRepository productRepository;

    public Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));
    }

    public Product resetStock(Long productId, Integer newStock) {
        Product product = getProduct(productId);
        product.setStockQuantity(newStock);
        return productRepository.save(product);
    }

    /**
     * AI-GENERATED BUGGY METHOD:
     * // TODO [Lab Task 2]: Fix Race Condition when 50 threads deduct stock simultaneously. Inventory goes negative!
     *
     * TẠI SAO KHO BỊ ÂM?
     * 1. Check-Then-Act Flaw: AI dùng `findById` để kiểm tra `stockQuantity < quantity` trên RAM (không dùng DB Lock).
     * 2. Thời điểm 50 luồng đồng thời ập đến, cả 50 luồng đều đọc stock = 10 -> CẢ 50 LUỒNG ĐỀU PASS ĐIỀU KIỆN!
     * 3. Sau khi pass, AI gọi câu lệnh UPDATE trừ trực tiếp trong DB: `stockQuantity = stockQuantity - 1`.
     * 4. Database tuần tự thực thi 50 lệnh trừ: 10 - 1 (x 50 lần) = -40!
     * -> KẾT QUẢ: 50 đơn hàng thành công, tồn kho bị ÂM NẶNG (-40)!
     */
    public boolean deductStock(Long productId, Integer quantity) {
        log.info("[Thread: {}] Attempting stock deduction for Product ID: {}, Quantity: {}",
                Thread.currentThread().getName(), productId, quantity);

        // 1. Kiểm tra tồn kho trên RAM (Không khóa hàng trong DB)
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        if (product.getStockQuantity() < quantity) {
            log.warn("[Thread: {}] Insufficient stock! Available: {}, Requested: {}",
                    Thread.currentThread().getName(), product.getStockQuantity(), quantity);
            return false;
        }

        // Giả lập độ trễ xử lý nghiệp vụ 40ms (thanh toán, gọi gateway, voucher)
        try {
            Thread.sleep(40);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 2. AI sinh ra câu lệnh UPDATE trực tiếp dưới DB nhưng THIẾU ĐIỀU KIỆN "AND p.stockQuantity >= :quantity"
        productRepository.deductStockDirect(productId, quantity);

        log.info("[Thread: {}] Stock deducted successfully via direct query.", Thread.currentThread().getName());
        return true;
    }
}
