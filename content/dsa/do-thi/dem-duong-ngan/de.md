# Đếm đường đi ngắn nhất

Cho đồ thị **vô hướng, không trọng số** *n* đỉnh, *m* cạnh và hai đỉnh *s*, *t*. Tìm độ dài đường đi ngắn nhất từ *s* tới *t* và **số đường đi ngắn nhất** khác nhau (chia lấy dư cho 10⁹ + 7).

Gợi ý: BFS từ *s*, giữ thêm `so[v]` = số đường ngắn nhất tới *v*. Khi đi cạnh *u* → *v*: nếu *v* chưa thăm thì `kc[v] = kc[u] + 1`, `so[v] = so[u]`; nếu *v* đã thăm với `kc[v] = kc[u] + 1` thì cộng thêm `so[u]` vào `so[v]`.

## Input

Dòng 1 là *n*, *m*, *s*, *t* (1 ≤ *n* ≤ 10⁵, 0 ≤ *m* ≤ 2·10⁵, 1 ≤ *s*, *t* ≤ *n*). *m* dòng sau, mỗi dòng hai đỉnh của một cạnh (không có cạnh lặp hay khuyên).

## Output

Hai số: độ dài đường đi ngắn nhất và số đường đi ngắn nhất mod 10⁹ + 7. Nếu không tới được *t* thì in `-1 0`.

## Ví dụ

Input:

```text
4 4 1 4
1 2
1 3
2 4
3 4
```

Output:

```text
2 2
```

Input:

```text
3 1 1 3
1 2
```

Output:

```text
-1 0
```
