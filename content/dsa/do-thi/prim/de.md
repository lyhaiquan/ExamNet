# Cây khung nhỏ nhất Prim

Cho đồ thị vô hướng **liên thông** *n* đỉnh (0 … *n* − 1), mỗi cạnh có trọng số. Một **cây khung** là tập *n* − 1 cạnh nối được mọi đỉnh mà không tạo chu trình. Tìm tổng trọng số nhỏ nhất của một cây khung (ví dụ: nối mạng cho *n* toà nhà với ít dây nhất).

**Thuật toán Prim:** cây mọc dần từ đỉnh 0. Mảng `kc`: `kc[v]` là trọng số cạnh nhẹ nhất nối *v* với **cây hiện tại** (−1 là chưa có cạnh nào). Lặp *n* lần:

1. Chọn đỉnh *u* **chưa vào cây** có `kc[u]` nhỏ nhất (lần đầu là đỉnh 0, `kc[0] = 0`). Cho *u* vào cây, cộng `kc[u]` vào tổng.
2. Với mỗi cạnh *u* – *v* trọng số *w*, *v* chưa vào cây: nếu `kc[v]` đang là −1 hoặc lớn hơn *w* thì `kc[v] = w` (cạnh nối *v* với cây giờ có thể đi qua *u*).

Khác Dijkstra ở bước 2: so với *w* (độ dài một cạnh nối vào cây), không phải `d[u] + w` (độ dài cả đường đi từ gốc). Cách duyệt mảng `kc` ở bước 1 mất O(*n*²).

## Input

Dòng 1 gồm *n* và *m* (1 ≤ *n* ≤ 1000, 0 ≤ *m* ≤ 10⁴). *m* dòng sau, mỗi dòng *u*, *v*, *w* (1 ≤ *w* ≤ 10⁴). Đồ thị liên thông, không có cạnh trùng.

## Output

Tổng trọng số của cây khung nhỏ nhất.

## Ví dụ

Input:

```text
5 7
0 1 2
0 3 6
1 2 3
1 3 8
1 4 5
2 4 7
3 4 9
```

Output:

```text
16
```

Cây khung nhỏ nhất gồm các cạnh 0–1 (2), 1–2 (3), 1–4 (5), 0–3 (6).
