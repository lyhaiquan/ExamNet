# Floyd–Warshall

Cho đồ thị **có hướng** *n* đỉnh (0 … *n* − 1), mỗi cạnh có độ dài không âm. Tìm độ dài đường đi ngắn nhất giữa **mọi cặp** đỉnh.

**Bảng `d`:** ban đầu `d[i][i] = 0`, `d[i][j]` là độ dài cạnh *i* → *j* (nhiều cạnh thì lấy cạnh ngắn nhất), không có cạnh thì vô cực (`float('inf')`, bảng hiện là ∞).

**Floyd–Warshall:** lần lượt cho phép đi qua đỉnh *k* = 0, 1, …, *n* − 1. Với mỗi cặp (*i*, *j*): đi từ *i* tới *k* rồi từ *k* tới *j* mà ngắn hơn `d[i][j]` thì cập nhật:

`d[i][j] = min(d[i][j], d[i][k] + d[k][j])`

Sau bước *k*, `d[i][j]` là đường ngắn nhất chỉ đi qua các đỉnh trung gian trong 0 … *k*. Hết *n* bước là đường ngắn nhất thật. Ba vòng lặp lồng nhau, **vòng *k* ở ngoài cùng**: O(*n*³).

## Input

Dòng 1 gồm *n* và *m* (1 ≤ *n* ≤ 50, 0 ≤ *m* ≤ 2500). *m* dòng sau, mỗi dòng *u*, *v*, *w*: cạnh *u* → *v* dài *w* (0 ≤ *w* ≤ 10⁴).

## Output

*n* dòng, dòng *i* gồm `d[i][0]` … `d[i][n-1]`; không đi tới được thì in −1.

## Ví dụ

Input:

```text
4 5
0 1 5
0 3 10
1 2 3
2 3 1
3 0 2
```

Output:

```text
0 5 8 9
6 0 3 4
3 8 0 1
2 7 10 0
```
