# Cây khung nhỏ nhất (đồ thị lớn)

Cho đồ thị **vô hướng** *n* đỉnh, *m* cạnh có trọng số. Tìm tổng trọng số của **cây khung nhỏ nhất**: tập *n* − 1 cạnh nối mọi đỉnh lại với nhau mà tổng trọng số nhỏ nhất. Nếu đồ thị không liên thông thì không có cây khung.

Gợi ý: **Kruskal**. Sắp các cạnh theo trọng số tăng dần, lần lượt lấy cạnh nối hai thành phần khác nhau. Dùng cấu trúc **hợp và tìm** (DSU, có nén đường đi) để kiểm hai đỉnh đã cùng thành phần chưa.

## Input

Dòng 1 là *n* và *m* (1 ≤ *n* ≤ 10⁵, 0 ≤ *m* ≤ 2·10⁵). *m* dòng sau, mỗi dòng `u v w` (|*w*| ≤ 10⁹).

## Output

Một số: tổng trọng số cây khung nhỏ nhất, hoặc `IMPOSSIBLE` nếu đồ thị không liên thông.

## Ví dụ

Input:

```text
4 5
1 2 3
2 3 1
3 4 4
1 4 2
1 3 5
```

Output:

```text
6
```

Input:

```text
3 1
1 2 5
```

Output:

```text
IMPOSSIBLE
```
