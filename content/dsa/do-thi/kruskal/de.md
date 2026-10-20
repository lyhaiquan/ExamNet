# Cây khung nhỏ nhất Kruskal, hợp và tìm

Cùng bài toán với Prim: tổng trọng số nhỏ nhất của một cây khung của đồ thị vô hướng **liên thông** *n* đỉnh (0 … *n* − 1).

**Thuật toán Kruskal:** xét các cạnh theo trọng số **tăng dần** (bằng nhau thì theo thứ tự trong input). Cạnh *u* – *v* nối hai đỉnh đang ở **hai mảnh khác nhau** thì lấy (gộp hai mảnh); nối hai đỉnh **cùng một mảnh** thì bỏ, vì sẽ tạo chu trình. Lấy đủ *n* − 1 cạnh là xong.

**Hợp và tìm** (disjoint set union) cho biết hai đỉnh có cùng mảnh không. Mảng `cha`: mỗi mảnh là một cây, `cha[v]` là cha của *v* trong cây đó, gốc thì `cha[r] = r`. Ban đầu mỗi đỉnh một mảnh: `cha[v] = v`.

- `tim(v)`: đi theo `cha` tới gốc. **Nén đường:** trên đường về, gán `cha` của mọi đỉnh đi qua thẳng vào gốc, lần sau tìm nhanh hơn.
- `hop(u, v)`: `a = tim(u)`, `b = tim(v)`; `a == b` là cùng mảnh; khác thì gán `cha[b] = a` (gộp mảnh của *v* vào mảnh của *u*).

## Input

Dòng 1 gồm *n* và *m* (1 ≤ *n* ≤ 2·10⁴, 0 ≤ *m* ≤ 5·10⁴). *m* dòng sau, mỗi dòng *u*, *v*, *w* (1 ≤ *w* ≤ 10⁴). Đồ thị liên thông, không có cạnh trùng.

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
