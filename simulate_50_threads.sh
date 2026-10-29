#!/bin/bash
# ==============================================================================
# R2S MasterClass - Simulate 50 Concurrent Buyers (Flash Sale Race Condition)
# ==============================================================================

BASE_URL="http://localhost:8080/api/v1/inventory"
PRODUCT_ID=1

echo "=========================================================="
echo " [BƯỚC 1]: Reset tồn kho Product #$PRODUCT_ID về 10 sản phẩm"
echo "=========================================================="
curl -s -X POST "$BASE_URL/reset/$PRODUCT_ID?stock=10"
echo ""

echo ""
echo "=========================================================="
echo " [BƯỚC 2]: Kích hoạt 50 THREADS ĐỒNG THỜI mua hàng qua API..."
echo "=========================================================="

SUCCESS_COUNT=0
FAIL_COUNT=0

for i in $(seq 1 50); do
  curl -s -X POST "$BASE_URL/deduct" \
    -H "Content-Type: application/json" \
    -d "{\"productId\": $PRODUCT_ID, \"quantity\": 1}" &
done

# Chờ toàn bộ 50 sub-process hoàn tất
wait

echo ""
echo "=========================================================="
echo " [BƯỚC 3]: Kiểm tra tồn kho sau khi 50 khách hàng tranh nhau mua"
echo "=========================================================="
curl -s "$BASE_URL/$PRODUCT_ID"
echo ""
echo "=========================================================="
echo "  Nếu thấy stockQuantity ÂM (< 0), bạn đã chứng kiến BUG 2 (Race Condition)!"
echo "=========================================================="
