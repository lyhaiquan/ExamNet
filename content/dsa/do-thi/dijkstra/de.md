# Đường đi ngắn nhất Dijkstra

Cho đồ thị vô hướng *n* đỉnh (đánh số 0 … *n* − 1), *m* cạnh, mỗi cạnh có độ dài không âm. Tìm độ dài đường đi ngắn nhất từ đỉnh 0 tới mọi đỉnh. Đỉnh không đi tới được thì in −1.

**Thuật toán Dijkstra.** Mảng `d`: `d[v]` là độ dài đường đi ngắn nhất **đã tìm được** từ 0 tới *v* (−1 là chưa tìm được đường nào). Ban đầu `d[0] = 0`. Mỗi đỉnh có hai trạng thái: chưa **chốt** hoặc đã chốt (`d[v]` chắc chắn là ngắn nhất, không đổi nữa). Lặp:

1. Trong các đỉnh chưa chốt có `d` khác −1, chọn đỉnh *u* có `d[u]` **nhỏ nhất**. Không còn đỉnh nào thì dừng.
2. Chốt *u*. Mọi đường khác tới *u* đều phải đi qua một đỉnh chưa chốt có `d` không nhỏ hơn `d[u]`, rồi cộng thêm các cạnh không âm, nên không thể ngắn hơn.
3. Với mỗi cạnh *u* – *v* độ dài *w*, *v* chưa chốt: đi qua *u* tới *v* dài `d[u] + w`. Nếu `d[v]` đang là −1 hoặc lớn hơn thì gán `d[v] = d[u] + w`.

Bước 1 duyệt cả mảng `d` nên mỗi vòng mất O(*n*), cả thuật toán O(*n*²). Bài `dijkstra-lon` dùng hàng đợi ưu tiên (`heapq`) để nhanh hơn trên đồ thị lớn.

## Input

Dòng 1 gồm *n* và *m* (1 ≤ *n* ≤ 1000, 0 ≤ *m* ≤ 10⁴). *m* dòng sau, mỗi dòng *u*, *v*, *w*: cạnh nối *u* ≠ *v* dài *w* (0 ≤ *w* ≤ 10⁴).

## Output

*n* số: `d[0]`, `d[1]`, …, `d[n-1]`.

## Ví dụ

Input:

```text
6 9
0 1 7
0 2 9
0 5 14
1 2 10
1 3 15
2 3 11
2 5 2
3 4 6
4 5 9
```

Output:

```text
0 7 9 20 20 11
```
