# Đếm cách đặt n quân hậu

Đếm số cách đặt *n* quân hậu lên bàn cờ *n* × *n* sao cho không hai quân nào ăn nhau: không cùng hàng, không cùng cột, không cùng đường chéo.

Gợi ý: **quay lui** đặt mỗi hàng một quân. Giữ ba tập (hoặc ba mặt nạ bit) các cột, đường chéo chính (*hàng* − *cột*) và đường chéo phụ (*hàng* + *cột*) đã có quân để kiểm một ô trong O(1). Với *n* = 12 có hơn 14 000 cách, cần cắt nhánh tốt.

## Input

Một dòng *n* (1 ≤ *n* ≤ 12).

## Output

Một số: số cách đặt.

## Ví dụ

Input:

```text
4
```

Output:

```text
2
```

Input:

```text
1
```

Output:

```text
1
```
