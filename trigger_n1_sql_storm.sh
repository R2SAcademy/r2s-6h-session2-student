#!/bin/bash
# ==============================================================================
# R2S MasterClass - Trigger 1,000 Orders N+1 Query Storm
# ==============================================================================

echo "=========================================================="
echo " [BƯỚC 1]: Seed 1,000 Đơn Hàng vào Database..."
echo "=========================================================="
curl -s -X POST "http://localhost:8080/api/v1/orders/seed?count=1000"
echo ""

echo ""
echo "=========================================================="
echo " [BƯỚC 2]: Gọi GET /api/v1/orders?status=PAID để kích hoạt N+1 SQL Storm!"
echo "  HÃY QUAN SÁT TERMINAL SPRING BOOT: 1,001+ SELECT QUERIES SẼ BẮN LIÊN TỤC!"
echo "=========================================================="
TIME_OUTPUT=$(curl -s -w "\nThời gian phản hồi API (Total Time): %{time_total}s\n" "http://localhost:8080/api/v1/orders?status=PAID" -o /dev/null)
echo "$TIME_OUTPUT"

echo "=========================================================="
echo " [BƯỚC 3]: Hoàn tất! Hãy kiểm tra terminal Spring Boot để xem Session Metrics & số lượng SQL."
echo "=========================================================="
