# Duyệt theo chiều rộng (BFS) tìm khoảng cách

Cho đồ thị vô hướng *n* đỉnh (đánh số 1 … *n*) và *m* cạnh. Độ dài một đường đi là số cạnh trên đường đi đó. Với mỗi đỉnh *v*, hãy tìm độ dài đường đi ngắn nhất từ đỉnh 1 tới *v*, hoặc −1 nếu không đi tới được.

Dùng **duyệt theo chiều rộng** với hàng đợi `q` và đồ thị `g` trong khung code:

1. `d[1] = 0`, các đỉnh khác `d[v] = -1` (chưa tới); đưa đỉnh 1 vào hàng đợi;
2. khi hàng đợi còn phần tử: lấy `u = q.deq()` ở đầu hàng. Với mỗi đỉnh `v` trong `g.ke(u)`, nếu `d[v] == -1` thì `d[v] = d[u] + 1` và đưa `v` vào cuối hàng, `q.enq(v)`.

Vì sao đúng: hàng đợi lấy ra các đỉnh theo đúng thứ tự khoảng cách tăng dần. Mọi đỉnh cách 1 đúng *k* cạnh đều được lấy ra trước mọi đỉnh cách *k* + 1 cạnh, nên lần đầu tiên chạm tới *v* là qua đường ngắn nhất.

Muốn thấy khoảng cách ngay trên hình thì gọi `g.nhan(v, d[v])` sau khi gán `d[v]`.

## Input

Dòng 1 là *n* và *m* (1 ≤ *n* ≤ 5·10⁴, 0 ≤ *m* ≤ 10⁵). *m* dòng sau, mỗi dòng hai đỉnh *u*, *v* của một cạnh (có thể có cạnh lặp, không có khuyên).

## Output

Một dòng: *n* số `d[1] d[2] … d[n]`.

## Ví dụ

Input:

```text
7 7
1 2
1 3
2 4
3 4
4 5
3 6
5 6
```

Output:

```text
0 1 1 2 3 2 -1
```
