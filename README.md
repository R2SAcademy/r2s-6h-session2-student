# HƯỚNG DẪN THỰC HÀNH BUỔI 2: CODE AUDIT, UNIT TEST & DOCKERIZE PRODUCTION
> **Chương trình:** 6H MasterClass — OJT Production Readiness | **R2S Academy**  
> **Chủ đề:** Tối ưu truy vấn N+1, Xử lý Concurrency Locking, Unit Test Mockito & Đóng gói Container tối ưu  
> **Thời lượng thực hành:** 120 phút (2 giờ) | **Định dạng:** Hands-on Lab Challenge  

---

## 1. TỔNG QUAN & MỤC TIÊU BÀI LAB

Trong các dự án phát triển phần mềm hiện đại, mã nguồn do AI Agent (Cursor, GitHub Copilot) khởi tạo thường chạy rất trơn tru trong môi trường cục bộ (Localhost) với lượng dữ liệu nhỏ. Tuy nhiên, khi đưa lên môi trường Production chịu tải thực tế, ứng dụng dễ gặp phải các **"lỗi tiềm ẩn nghiêm trọng"** có thể làm sập hệ cơ sở dữ liệu hoặc gây thất thoát doanh thu.

Sau khi hoàn thành bài lab trong 2 giờ, học viên sẽ:
1. **Làm chủ kỹ năng Code Audit & Profiling:** Nhận diện và triệt tiêu dứt điểm lỗi **N+1 Query** của Hibernate ORM bằng kỹ thuật **JPQL `JOIN FETCH`**, giảm thiểu hơn 97% số lượng round-trip truy vấn xuống CSDL.
2. **Kiểm soát xung đột dữ liệu đồng thời (Concurrency Control):** Tái hiện lỗi **Race Condition** gây âm kho hàng (Inventory Underflow) khi có nhiều luồng cùng mua hàng, khắc phục triệt để bằng **Pessimistic Locking (`SELECT ... FOR UPDATE`)**.
3. **Thành thạo Unit Testing chuẩn Enterprise:** Viết kiểm thử tự động với **JUnit 5** và **Mockito**, cô lập hoàn toàn tầng Service và đo lường độ phủ mã nguồn bằng **JaCoCo** đạt **Code Coverage >= 80%**.
4. **Đóng gói ứng dụng chuẩn Production Ready:** Thiết kế **Multi-stage `Dockerfile`** áp dụng kỹ thuật Spring Boot Layertools và DevSecOps (Non-root user), nén dung lượng Docker Image từ **>800MB xuống dưới 120MB**.
5. **Vượt qua Quality Gate:** Tự tin tạo Pull Request chuẩn mực trên GitHub để Mentor Tech Lead nghiệm thu và trao chứng chỉ **Digital Badge *AI-Powered Junior Readiness***.

---

## 2. PHÂN BỔ THỜI LƯỢNG THỰC HÀNH (120 PHÚT)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 15 phút: Setup Môi trường & Khởi chạy CSDL qua Docker Compose                          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 25 phút: Task 1 - Audit & Fix lỗi N+1 Query (JPQL JOIN FETCH)                          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 25 phút: Task 2 - Tái hiện & Fix Race Condition âm kho (Pessimistic Locking)          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 30 phút: Task 3 - Viết Unit Test JUnit 5 + Mockito đạt JaCoCo Coverage >= 80%          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 15 phút: Task 4 - Đóng gói Multi-stage Dockerfile chuẩn Production (< 120MB)          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 10 phút: Task 5 - Git Commit Conventional, Tạo Pull Request & Nghiệm thu 1-1           │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. CHUẨN BỊ MÔI TRƯỜNG & KHỞI CHẠY (SETUP GUIDE)

### Bước 1: Mở dự án trong Cursor IDE / VS Code / IntelliJ IDEA
1. Mở IDE và chọn thư mục `r2s-6h-session2-student`.
2. Mở cửa sổ Terminal trong IDE (`Ctrl + ~` hoặc `Cmd + ~`).
3. Đảm bảo máy đã cài đặt JDK 17+ và Docker Desktop đang chạy.

### Bước 2: Khởi chạy CSDL PostgreSQL bằng Docker Compose
Dự án đã chuẩn bị sẵn file `docker-compose.yml`. Chạy lệnh sau để khởi động CSDL:
```bash
docker compose up -d
```
*(Nếu môi trường của bạn chưa cài docker compose, bạn có thể chạy lệnh tương đương:)*
```bash
docker run -d \
  --name r2s-postgres-student \
  -e POSTGRES_DB=ecommerce_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgrespassword \
  -p 5432:5432 \
  postgres:16-alpine
```

### Bước 3: Xác thực cấu hình `application.yml`
Kiểm tra file `src/main/resources/application.yml` để chắc chắn kết nối tới PostgreSQL chính xác:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_db
    username: postgres
    password: postgrespassword
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        generate_statistics: true
```

### Bước 4: Khởi chạy ứng dụng lần đầu
```bash
mvn spring-boot:run
```
Khi ứng dụng khởi động thành công tại cổng `8080`, hệ thống tự động khởi tạo dữ liệu mẫu ban đầu gồm:
- 1 sản phẩm Flash Sale iPhone 15 Pro (tồn kho ban đầu = 10)
- 2 sản phẩm điện tử phụ trợ
- 50 đơn hàng mẫu gắn liền với các Order Items.

---

## 4. CHI TIẾT 5 NHIỆM VỤ THỰC HÀNH (CORE LAB TASKS)

---

### NHIỆM VỤ 1: CODE AUDIT & TRIỆT TIÊU LỖI N+1 QUERY (25 PHÚT)

#### 1. Hiện trạng lỗi (Bug Discovery)
Trong `OrderService.java`, AI đã sinh ra hàm `getAllOrdersWithDetails()` sử dụng phương thức mặc định `orderRepository.findByStatus(status)`. Sau đó, vòng lặp `for` duyệt qua từng Order và gọi `order.getItems()` cùng `item.getProduct().getName()`.

#### 2. Tái hiện lỗi N+1 Query
1. Mở tab Terminal mới và gửi request lấy danh sách đơn hàng đã thanh toán:
   ```bash
   curl -s "http://localhost:8080/api/v1/orders?status=PAID" -o /dev/null
   ```
   *(Hoặc chạy script kích hoạt tải 1,000 đơn hàng: `./trigger_n1_sql_storm.sh`)*
2. **Quan sát Console Log của ứng dụng:**
   Bạn sẽ thấy Hibernate liên tục bắn ra hàng chục đến hàng trăm câu SQL nối tiếp:
   - 1 câu SQL gốc: `SELECT * FROM orders WHERE status = ?`
   - N câu SQL con: `SELECT * FROM order_items WHERE order_id = ?`
   - N x M câu SQL con: `SELECT * FROM products WHERE id = ?`
   > **Tác hại:** Nếu hệ thống có 10,000 đơn hàng, việc này tạo ra 20,001 câu truy vấn round-trip đến database, chiếm dụng toàn bộ Connection Pool và gây nghẽn CPU database.

#### 3. Yêu cầu kỹ thuật cần đạt:
1. Mở file `src/main/java/com/r2s/masterclass/repository/OrderRepository.java`.
2. Định nghĩa một phương thức truy vấn JPQL tùy biến sử dụng từ khóa **`JOIN FETCH`** kết hợp **`DISTINCT`**:
   - `LEFT JOIN FETCH o.items i`
   - `LEFT JOIN FETCH i.product`
3. Cập nhật lại `OrderService.java` để gọi phương thức tối ưu này.
4. **Tiêu chuẩn nghiệm thu:** Gọi lại API `GET /api/v1/orders?status=PAID` và kiểm tra Console Log: Hệ thống **chỉ được phép thực thi DUY NHẤT 1 câu lệnh SQL**.

---

### NHIỆM VỤ 2: FIX RACE CONDITION & CHỐNG ÂM KHO HÀNG (25 PHÚT)

#### 1. Hiện trạng lỗi (Bug Discovery)
Trong `InventoryService.java`, hàm `deductStock()` kiểm tra số lượng tồn kho trên bộ nhớ tạm (RAM) của JVM bằng `product.getStockQuantity() < quantity` rồi thực thi lệnh trừ kho trực tiếp dưới database mà không sử dụng cơ chế khóa (Lock).

#### 2. Tái hiện lỗi âm kho (Inventory Underflow)
1. Mở file test: `src/test/java/com/r2s/masterclass/StudentInventoryConcurrencyTest.java`.
2. Chạy test case này (bằng nút Run trong IDE hoặc lệnh `mvn test -Dtest=StudentInventoryConcurrencyTest`).
3. Bài test giả lập **50 khách hàng (Threads) tranh mua đồng thời một sản phẩm chỉ còn 10 món trong kho**.
4. **Kết quả quan sát được:**
   ```
   Tồn kho ban đầu             : 10
   Số luồng cùng mua           : 50
   Số đơn báo thành công       : 50
   TỒN KHO THỰC TẾ CUỐI CÙNG   : -40  <-- (LỖI ÂM KHO NGHIÊM TRỌNG!)
   ```
   *(Bạn cũng có thể chạy file `./simulate_50_threads.sh` khi server đang chạy để thấy lỗi qua REST API).*

#### 3. Yêu cầu kỹ thuật cần đạt:
1. Mở file `src/main/java/com/r2s/masterclass/repository/ProductRepository.java`.
2. Thêm phương thức tìm kiếm sản phẩm áp dụng khóa bi quan (Pessimistic Locking):
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   @Query("SELECT p FROM Product p WHERE p.id = :id")
   Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
   ```
3. Cập nhật `InventoryService.deductStock()`:
   - Thêm annotation `@Transactional(isolation = Isolation.READ_COMMITTED)`.
   - Dùng `findByIdWithPessimisticLock(productId)` để khóa dòng dữ liệu độc quyền (`SELECT ... FOR UPDATE`).
   - Kiểm tra tồn kho, trừ số lượng và gọi `productRepository.save(product)`.
4. **Tiêu chuẩn nghiệm thu:** Chạy lại bài test hoặc script giả lập: **Tồn kho cuối cùng phải bằng đúng 0, đúng 10 giao dịch báo thành công và 40 giao dịch còn lại nhận thông báo từ chối an toàn.**

---

### NHIỆM VỤ 3: VIẾT UNIT TEST JUNIT 5 & MOCKITO ĐẠT COVERAGE >= 80% (30 PHÚT)

#### 1. Yêu cầu mục tiêu
Để đảm bảo chất lượng kiểm thử ở mức độ Production và ngăn ngừa lỗi hồi quy (regression), học viên cần viết Unit Test cho tầng nghiệp vụ (`com.r2s.masterclass.service`), cô lập hoàn toàn database bằng cách giả lập (mocking) tầng Repository.

#### 2. Các file test cần hoàn thiện:
Khung test mẫu đã được tạo sẵn tại:
1. `src/test/java/com/r2s/masterclass/service/OrderServiceTest.java`:
   - Test case `getAllOrdersWithDetails`: Kiểm tra ánh xạ DTO chính xác khi có dữ liệu đơn hàng.
   - Test case `getAllOrdersWithDetails`: Kiểm tra khi danh sách rỗng (empty list).
   - Test case `seedOrders`: Kiểm tra quá trình sinh đơn hàng mẫu khi đã có hoặc chưa có sản phẩm.
2. `src/test/java/com/r2s/masterclass/service/InventoryServiceTest.java`:
   - Test case `deductStock`: Trừ kho thành công khi tồn kho đủ (`stockQuantity >= requested`).
   - Test case `deductStock`: Từ chối trừ kho và trả về `false` khi hết hàng (`insufficient stock`).
   - Test case `deductStock`: Ném ngoại lệ `IllegalArgumentException` khi truyền `productId` không tồn tại.
   - Test case `getProduct` & `resetStock`: Kiểm tra các hàm nghiệp vụ đọc và cập nhật tồn kho.

#### 3. Kiểm tra Code Coverage bằng JaCoCo:
Chạy lệnh kiểm tra coverage tự động:
```bash
mvn clean test jacoco:report
```
- Mở file báo cáo HTML tại trình duyệt: `target/site/jacoco/index.html`.
- Kiểm tra gói `com.r2s.masterclass.service`: Độ phủ dòng lệnh (**Line Coverage**) và độ phủ nhánh (**Branch Coverage**) phải đạt **tối thiểu 80%**.
- Chạy lệnh xác thực Quality Gate:
  ```bash
  mvn verify
  ```
  *(Nếu độ phủ chưa đạt 80%, JaCoCo sẽ chặn build với thông báo lỗi rõ ràng).*

---

### NHIỆM VỤ 4: ĐÓNG GÓI DOCKER MULTI-STAGE NÉN IMAGE < 120MB (15 PHÚT)

#### 1. Hiện trạng & Yêu cầu
Nếu viết Dockerfile đơn tầng kèm đầy đủ Maven và JDK, kích thước image tạo ra sẽ vượt quá **800MB - 1GB**, gây tốn băng thông CI/CD và tiềm ẩn rủi ro bảo mật do chứa các công cụ biên dịch không cần thiết.

#### 2. Các bước triển khai trong `Dockerfile`:
Mở file `Dockerfile` tại thư mục gốc và hoàn thiện cấu trúc 2 tầng (Multi-stage build):
1. **Stage 1 (Builder Stage):**
   - Base image: `maven:3.9.6-eclipse-temurin-17-alpine AS builder`.
   - Tối ưu Docker Cache: Copy file `pom.xml` trước và chạy `mvn dependency:go-offline -B`.
   - Copy mã nguồn `src` và build ứng dụng: `mvn clean package -DskipTests`.
   - Giải nén Spring Boot Layertools:  
     `RUN java -Djarmode=layertools -jar target/*.jar extract`.
2. **Stage 2 (Runtime Stage - Chuẩn DevSecOps):**
   - Base image: `eclipse-temurin:17-jre-alpine AS runtime`.
   - Tạo non-root user:  
     `RUN addgroup -S appgroup && adduser -S appuser -G appgroup`.
   - Copy các layers từ Stage 1 theo thứ tự: `dependencies`, `spring-boot-loader`, `snapshot-dependencies`, `application`.
   - Phân quyền thư mục cho `appuser` và chuyển ngữ cảnh sang `USER appuser`.
   - Bổ sung `HEALTHCHECK` thăm dò endpoint `/actuator/health`.
   - Thiết lập `ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]`.

#### 3. Kiểm tra dung lượng Image:
Chạy lệnh build và kiểm tra dung lượng:
```bash
docker build -t r2s-order-service:v1 .
docker images | grep r2s-order-service
```
> **Tiêu chuẩn nghiệm thu:** Cột `SIZE` của image `r2s-order-service:v1` phải **nhỏ hơn 120MB**!

---

### NHIỆM VỤ 5: QUALITY GATE & NỘP BÀI (10 PHÚT)

1. **Kiểm tra an toàn bảo mật:** Đảm bảo không commit các file nhạy cảm (`.env`, secret keys) hoặc thư mục build (`target/`).
2. **Commit mã nguồn theo chuẩn Conventional Commits:**
   ```bash
   git add .
   git commit -m "feat: resolve N+1 query, fix race condition with pessimistic lock, add unit tests and multi-stage dockerfile"
   ```
3. **Mở Pull Request (PR):**
   - Đẩy nhánh làm việc lên GitHub cá nhân của bạn.
   - Gửi đường dẫn Pull Request cho Mentor Tech Lead.
   - Mentor sẽ thực hiện Code Review 1-1 trực tiếp, kiểm tra log SQL, test coverage và đối chiếu barem điểm để cấp **Digital Badge *AI-Powered Junior Readiness***!

---

## 5. THỬ THÁCH NÂNG CAO CHO DỰ ÁN THỰC TẾ (PRODUCTION ADD-ONS)

*Dành cho học viên hoàn thành sớm bài lab và muốn nâng cấp source code thành module dự án thực tế trong CV cá nhân theo chuẩn Production.*

---

### MODULE 1: LƯU VẾT BIẾN ĐỘNG KHO HÀNG (INVENTORY AUDIT TRAIL)

#### 1. Mục tiêu & Bối cảnh Production (Problem & Business Context)
Trong hệ thống e-Commerce / ERP thực tế, việc trừ tồn kho thành công là chưa đủ. Mọi biến động kho (trừ hàng khi khách đặt mua, hoàn kho khi hủy đơn, nhập kho bổ sung) bắt buộc phải có vết kiểm toán (Audit Trail) để đối soát kế toán tài chính, truy vết sai lệch số liệu và chống gian lận. Thao tác ghi log kiểm toán phải nằm trong cùng ranh giới Transaction với thao tác trừ kho (nếu trừ kho lỗi thì log cũng phải rollback).

#### 2. Đặc tả Thiết kế Kỹ thuật (Technical Specification)
1. **Entity:** Tạo file `src/main/java/com/r2s/masterclass/domain/InventoryAuditLog.java`
   - Bảng: `inventory_audit_logs`
   - Thuộc tính:
     - `Long id`: Khóa chính (`@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`)
     - `Long productId`: ID sản phẩm biến động (`@Column(name = "product_id", nullable = false)`)
     - `String action`: Loại hành động (`@Column(nullable = false)`) — ví dụ: `"DEDUCT"`, `"RESTOCK"`
     - `Integer changedQuantity`: Số lượng thay đổi (`@Column(nullable = false)`)
     - `Integer previousStock`: Tồn kho trước khi đổi (`@Column(nullable = false)`)
     - `Integer newStock`: Tồn kho sau khi đổi (`@Column(nullable = false)`)
     - `LocalDateTime createdAt`: Thời điểm ghi nhận (`@Column(nullable = false)`)
2. **Repository:** Tạo file `src/main/java/com/r2s/masterclass/repository/InventoryAuditLogRepository.java`
   - Kế thừa `JpaRepository<InventoryAuditLog, Long>`
   - Khai báo phương thức truy vấn:
     ```java
     List<InventoryAuditLog> findByProductIdOrderByCreatedAtDesc(Long productId);
     ```
3. **DTO:** Tạo file `src/main/java/com/r2s/masterclass/dto/InventoryAuditLogResponseDTO.java`:
   ```java
   package com.r2s.masterclass.dto;

   import lombok.Builder;
   import lombok.Getter;
   import java.time.LocalDateTime;

   @Getter
   @Builder
   public class InventoryAuditLogResponseDTO {
       private Long logId;
       private Long productId;
       private String action;
       private Integer changedQuantity;
       private Integer previousStock;
       private Integer newStock;
       private LocalDateTime createdAt;
   }
   ```
4. **Endpoint Controller:** Bổ sung vào `InventoryController.java`
   - Method: `GET /api/v1/inventory/{productId}/audit-logs`
   - Trả về: `ResponseEntity<List<InventoryAuditLogResponseDTO>>`

#### 3. Hướng dẫn Triển khai từng bước (Guided Blueprint & Traps)
- **Bước 1 (Tạo Entity & Repository):** Tạo Entity `InventoryAuditLog` với các annotation `@Entity`, `@Table(name = "inventory_audit_logs")`, `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.
- **Bước 2 (Ghi log trong Transaction):**
  Trong `InventoryService.java`, inject `InventoryAuditLogRepository`. Tại hàm `deductStock(Long productId, Integer quantity)` sau khi trừ kho thành công:
  ```java
  // Blueprint ghi log trong cùng Transaction:
  InventoryAuditLog auditLog = InventoryAuditLog.builder()
          .productId(productId)
          .action("DEDUCT")
          .changedQuantity(quantity)
          .previousStock(product.getStockQuantity() + quantity)
          .newStock(product.getStockQuantity())
          .createdAt(LocalDateTime.now())
          .build();
  auditLogRepository.save(auditLog);
  ```
- **Bước 3 (Expose API & Service query):** Bổ sung hàm `getAuditLogs(Long productId)` trong `InventoryService` và gọi tại `InventoryController`.
- **Cảnh báo bẫy kỹ thuật (Traps):**
  - *Trap 1 (Lệch giao dịch - Inconsistency):* Tuyệt đối không dùng `@Async` hoặc mở một transaction riêng cho việc ghi log audit. Nếu việc trừ kho bị rollback (do lỗi thanh toán phía sau), audit log phải tự động rollback theo.
  - *Trap 2 (Thiếu Index):* Bảng audit log tăng trưởng rất nhanh. Hãy thêm annotation `@Table(name = "inventory_audit_logs", indexes = {@Index(name = "idx_audit_product_id", columnList = "product_id")})` để tránh Full Table Scan khi truy vấn.

#### 4. Tiêu chuẩn Nghiệm thu & Demo (Acceptance Criteria & Verification)
1. Kích hoạt giao dịch mua hàng:
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/inventory/deduct \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 2}'
   ```
2. Gọi API truy vấn lịch sử biến động kho:
   ```bash
   curl -s http://localhost:8080/api/v1/inventory/1/audit-logs
   ```
3. **Kết quả đạt chuẩn:** Trả về mã HTTP 200 kèm mảng JSON chứa bản ghi có `previousStock: 10`, `newStock: 8`, `changedQuantity: 2`, `action: "DEDUCT"`.

---

### MODULE 2: PHÂN TRANG AN TOÀN KHI DÙNG JOIN FETCH (SAFE PAGINATION)

#### 1. Mục tiêu & Bối cảnh Production (Problem & Business Context)
Bảng `orders` trong thực tế có hàng triệu bản ghi, do đó API bắt buộc phải hỗ trợ phân trang (`Pageable: page, size`). Tuy nhiên, nếu lập trình viên dùng `JOIN FETCH` một Collection (`o.items`) cùng với `Pageable`, Hibernate sẽ đưa ra cảnh báo nguy hiểm: `HHH000104: firstResult/maxResults specified with collection fetch; applying in memory!`. Hibernate buộc phải kéo toàn bộ dữ liệu bảng về RAM rồi mới phân trang trên RAM $\rightarrow$ Gây nghẽn mạng và sập ứng dụng vì lỗi `OutOfMemoryError` (OOM).

#### 2. Đặc tả Thiết kế Kỹ thuật (Technical Specification)
1. **Entity `Order.java`:** Cấu hình `@BatchSize(size = 50)` trên Collection `items` (Lưu ý: Import chính xác từ package Hibernate `org.hibernate.annotations.BatchSize`, không phải của JPA chuẩn):
   ```java
   import org.hibernate.annotations.BatchSize;

   @BatchSize(size = 50)
   @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<OrderItem> items = new ArrayList<>();
   ```
2. **Repository `OrderRepository.java`:** Định nghĩa truy vấn phân trang KHÔNG chứa JOIN FETCH collection:
   ```java
   @Query("SELECT o FROM Order o WHERE o.status = :status ORDER BY o.createdAt DESC")
   Page<Order> findOrdersPaged(@Param("status") OrderStatus status, Pageable pageable);
   ```
3. **DTO Response:** Tái sử dụng `OrderResponseDTO` và đóng gói dưới dạng `Page<OrderResponseDTO>`.
4. **Endpoint Controller:** Bổ sung vào `OrderController.java`
   - Method: `GET /api/v1/orders/paged`
   - Params: `status` (mặc định: `PAID`), `page` (mặc định: `0`), `size` (mặc định: `10`)

#### 3. Hướng dẫn Triển khai từng bước (Guided Blueprint & Traps)
- **Bước 1 (Cấu hình Batch Fetching):** Mở `src/main/java/com/r2s/masterclass/domain/Order.java`, thêm annotation `org.hibernate.annotations.BatchSize(size = 50)` ngay trên trường `items`.
- **Bước 2 (Viết Query Pageable):** Mở `OrderRepository.java`, viết method `findOrdersPaged` như mục 2 ở trên. Chú ý chỉ `SELECT o FROM Order o`, tuyệt đối không `JOIN FETCH o.items`.
- **Bước 3 (Xử lý chuyển đổi DTO ở Service):**
  Trong `OrderService.java`, viết hàm:
  ```java
  @Transactional(readOnly = true)
  public Page<OrderResponseDTO> getOrdersPaged(OrderStatus status, Pageable pageable) {
      Page<Order> orderPage = orderRepository.findOrdersPaged(status, pageable);
      return orderPage.map(order -> {
          // Khi gọi order.getItems(), Hibernate tự động gom 50 Orders lại và fetch items bằng 1 câu SQL:
          // SELECT * FROM order_items WHERE order_id IN (?, ?, ..., ?)
          List<OrderItemResponseDTO> itemDTOs = order.getItems().stream()
                  .map(item -> OrderItemResponseDTO.builder()
                          .itemId(item.getId())
                          .productId(item.getProduct().getId())
                          .productName(item.getProduct().getName())
                          .quantity(item.getQuantity())
                          .unitPrice(item.getUnitPrice())
                          .build())
                  .collect(Collectors.toList());

          return OrderResponseDTO.builder()
                  .orderId(order.getId())
                  .orderCode(order.getOrderCode())
                  .customerEmail(order.getCustomerEmail())
                  .status(order.getStatus())
                  .totalAmount(order.getTotalAmount())
                  .createdAt(order.getCreatedAt())
                  .items(itemDTOs)
                  .build();
      });
  }
  ```
- **Cảnh báo bẫy kỹ thuật (Traps):**
  - *Trap 1 (Bẫy kinh điển):* Nếu cố tình viết `JOIN FETCH o.items` trong hàm có `Pageable`, Hibernate sẽ hoàn toàn bỏ qua `LIMIT/OFFSET` dưới SQL CSDL và kéo hàng ngàn dòng về bộ nhớ JVM. Hãy kiểm tra console log để đảm bảo không xuất hiện dòng chữ `HHH000104`.
  - *Trap 2:* Nếu quên `@BatchSize`, mỗi khi gọi `order.getItems()` cho 10 đơn hàng trên trang, hệ thống lại quay về lỗi N+1 Query (bắn 10 câu SQL con). `@BatchSize(size = 50)` sẽ gom toàn bộ về đúng 1 câu SQL duy nhất với mệnh đề `WHERE order_id IN (...)`.

#### 4. Tiêu chuẩn Nghiệm thu & Demo (Acceptance Criteria & Verification)
1. Gửi request phân trang lấy 5 đơn hàng trang đầu tiên:
   ```bash
   curl -s "http://localhost:8080/api/v1/orders/paged?page=0&size=5&status=PAID"
   ```
2. **Kiểm chứng Console Log Spring Boot:**
   - Tuyệt đối **KHÔNG có cảnh báo `HHH000104`**.
   - Câu truy vấn lấy Orders có mệnh đề: `LIMIT ? OFFSET ?`.
   - Câu truy vấn lấy Order Items có mệnh đề: `WHERE order_id IN (?, ?, ?, ?, ?)`.
   - Kết quả JSON trả về đúng 5 phần tử kèm cấu trúc metadata chuẩn Spring Data (`totalElements`, `totalPages`, `number`, `size`).

---

### MODULE 3: ĐỐI CHIẾU OPTIMISTIC LOCKING (@VERSION) & CƠ CHẾ RETRY

#### 1. Mục tiêu & Bối cảnh Production (Problem & Business Context)
Pessimistic Lock (`SELECT ... FOR UPDATE`) khóa chặt bản ghi trong CSDL cho đến khi transaction hoàn tất. Khi có 10,000 khách hàng tranh mua sản phẩm thông thường, việc giữ connection khóa dòng sẽ làm cạn kiệt Connection Pool (HikariCP Timeout). Giải pháp thay thế cho các hệ thống phân tán là **Optimistic Locking (Khóa lạc quan)** dùng `@Version`: CSDL không khóa dòng khi đọc, chỉ kiểm tra version khi UPDATE. Nếu có xung đột, hệ thống sẽ tự động thử lại (Retry with Exponential Backoff).

#### 2. Đặc tả Thiết kế Kỹ thuật (Technical Specification)
1. **Entity `Product.java`:** Thêm trường version kiểm soát xung đột:
   ```java
   @Version
   private Long version;
   ```
2. **DTO Request:** Tái sử dụng `StockDeductRequestDTO` (`productId`, `quantity`).
3. **Cấu hình Dependency trong `pom.xml` (Chọn 1 trong 2 hướng tiếp cận):**
   - **Cách A (Khuyến nghị chuẩn Enterprise - Dùng Spring Retry):**
     Bổ sung 2 dependency sau vào `pom.xml`:
     ```xml
     <!-- Spring Retry & Spring AOP -->
     <dependency>
         <groupId>org.springframework.retry</groupId>
         <artifactId>spring-retry</artifactId>
     </dependency>
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-aop</artifactId>
     </dependency>
     ```
     Kích hoạt tại `R2SApplication.java`: Thêm annotation `@EnableRetry`.
   - **Cách B (Manual Retry Loop - Thuật toán thuần, không cần dependency mới):**
     Không cần sửa `pom.xml`, nhưng bắt buộc phải tách Worker Class để tránh lỗi Spring AOP Self-Invocation (chi tiết ở Bước 3B).
4. **Endpoint Controller:** Bổ sung vào `InventoryController.java`
   - Method: `POST /api/v1/inventory/deduct-optimistic`
   - Request Body: `{"productId": 1, "quantity": 1}`

#### 3. Hướng dẫn Triển khai từng bước (Guided Blueprint & Traps)
- **Bước 1 (Thêm `@Version`):** Mở `Product.java`, thêm trường `jakarta.persistence.Version`:
  ```java
  @Version
  private Long version;
  ```

- **Bước 2A (Triển khai theo Cách A - Dùng Spring Retry Declarative):**
  Trong `InventoryService.java`, viết hàm trừ kho kết hợp `@Retryable`:
  ```java
  @Retryable(
      retryFor = org.springframework.orm.ObjectOptimisticLockingFailureException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 50, multiplier = 2.0)
  )
  @Transactional
  public boolean deductStockOptimistic(Long productId, Integer quantity) {
      Product product = productRepository.findById(productId)
              .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

      if (product.getStockQuantity() < quantity) {
          return false;
      }

      product.setStockQuantity(product.getStockQuantity() - quantity);
      productRepository.save(product);
      return true;
  }
  ```

- **Bước 2B (Triển khai theo Cách B - Thuật toán thuần & Xử lý Triệt để Bẫy Self-Invocation):**
  Để mỗi lần thử lại mở một Transaction mới hoàn toàn mà không bị dính cờ `Rollback-Only`, ta tách riêng Transaction Worker:
  1. Tạo file Worker: `src/main/java/com/r2s/masterclass/service/InventoryOptimisticWorker.java`
     ```java
     package com.r2s.masterclass.service;

     import com.r2s.masterclass.domain.Product;
     import com.r2s.masterclass.repository.ProductRepository;
     import lombok.RequiredArgsConstructor;
     import org.springframework.stereotype.Component;
     import org.springframework.transaction.annotation.Propagation;
     import org.springframework.transaction.annotation.Transactional;

     @Component
     @RequiredArgsConstructor
     public class InventoryOptimisticWorker {
         private final ProductRepository productRepository;

         @Transactional(propagation = Propagation.REQUIRES_NEW)
         public boolean deductSingle(Long productId, Integer quantity) {
             Product product = productRepository.findById(productId)
                     .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

             if (product.getStockQuantity() < quantity) {
                 return false;
             }

             product.setStockQuantity(product.getStockQuantity() - quantity);
             productRepository.save(product);
             return true;
         }
     }
     ```
  2. Trong `InventoryService.java`, inject `InventoryOptimisticWorker` và xây dựng vòng lặp Retry độc lập:
     ```java
     private final InventoryOptimisticWorker optimisticWorker;

     public boolean deductStockOptimisticWithRetry(Long productId, Integer quantity, int maxRetries) {
         int attempts = 0;
         long backoffMs = 40;

         while (attempts < maxRetries) {
             try {
                 // Gọi qua Spring Bean proxy của Worker -> Mỗi lần retry là 1 Transaction mới hoàn toàn
                 return optimisticWorker.deductSingle(productId, quantity);
             } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                 attempts++;
                 log.warn("[Thread: {}] Xung đột phiên bản lần {}/{}. Thử lại sau {}ms...",
                         Thread.currentThread().getName(), attempts, maxRetries, backoffMs);
                 if (attempts >= maxRetries) {
                     log.error("[Thread: {}] Vượt quá số lần thử lại!", Thread.currentThread().getName());
                     return false;
                 }
                 try {
                     Thread.sleep(backoffMs);
                     backoffMs *= 2; // Exponential Backoff
                 } catch (InterruptedException ie) {
                     Thread.currentThread().interrupt();
                     return false;
                 }
             }
         }
         return false;
     }
     ```

-  **Cảnh báo bẫy kỹ thuật (Traps):**
  - *Trap 1 (Bẫy Spring AOP Self-Invocation kinh điển):* Nếu bạn viết cả hàm `deductSingle` (@Transactional) và vòng lặp `while/retry` trong cùng một class `InventoryService` rồi gọi `this.deductSingle()`, Spring AOP Proxy sẽ bị vô hiệu hóa (bypassed). Hậu quả: `@Transactional` không hoạt động, transaction bị đánh dấu Rollback-Only và ném `UnexpectedRollbackException`. Tách riêng `InventoryOptimisticWorker` như Bước 2B là giải pháp chuẩn Production!
  - *Trap 2 (Xung đột Dependency):* Nếu chọn Cách A, hãy đảm bảo bổ sung đủ cả 2 dependency `spring-retry` và `spring-boot-starter-aop` (Spring Retry bắt buộc cần Spring AOP để hoạt động).

#### 4. Tiêu chuẩn Nghiệm thu & Demo (Acceptance Criteria & Verification)
1. Gửi request trừ kho lạc quan:
   ```bash
   curl -s -X POST http://localhost:8080/api/v1/inventory/deduct-optimistic \
     -H "Content-Type: application/json" \
     -d '{"productId": 1, "quantity": 1}'
   ```
2. **Kiểm chứng Database:**
   - Quan sát giá trị cột `version` trong bảng `products` tăng dần sau mỗi lượt trừ kho thành công (`version = 0, 1, 2, ...`).
3. **Thuyết trình đối chiếu trước Mentor:**
   - *Pessimistic Lock:* Dành cho bài toán tranh chấp cực cao (High contention - Flash sale hàng hiếm), chấp nhận xếp hàng để đảm bảo 100% dữ liệu nhất quán.
   - *Optimistic Lock + Retry:* Dành cho bài toán tranh chấp trung bình/thấp (Low contention), lưu lượng truy cập lớn (Throughput cao), không chiếm dụng kết nối database lâu.

---
*Chúc các bạn hoàn thành xuất sắc bài Lab thực hành 2 giờ! Mọi thắc mắc trong quá trình debug hãy giơ tay để Mentor hỗ trợ trực tiếp 1-1.*
