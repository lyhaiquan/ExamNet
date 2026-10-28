# Đường đi ngắn nhất (đồ thị lớn)

Cho đồ thị **có hướng** *n* đỉnh, *m* cạnh, mỗi cạnh có trọng số không âm. Tính độ dài đường đi ngắn nhất từ đỉnh 1 tới mọi đỉnh.

Với *n*, *m* tới 2·10⁵, Dijkstra tìm đỉnh gần nhất bằng cách duyệt cả mảng (O(*n*²)) sẽ quá chậm. Dùng **đống** (`heapq`): đẩy (khoảng cách, đỉnh) vào đống, mỗi lần lấy ra đỉnh gần nhất; gặp lại đỉnh đã chốt thì bỏ qua. Độ phức tạp O((*n* + *m*) log *m*).

## Input

Dòng 1 là *n* và *m* (1 ≤ *n*, *m* ≤ 2·10⁵). *m* dòng sau, mỗi dòng `u v w`: cạnh từ *u* tới *v* trọng số *w* (0 ≤ *w* ≤ 10⁹). Có thể có nhiều cạnh giữa hai đỉnh và cạnh từ một đỉnh tới chính nó.

## Output

Một dòng *n* số: khoảng cách ngắn nhất từ đỉnh 1 tới đỉnh 1, 2, …, *n*; đỉnh không tới được thì in −1.

## Ví dụ

Input:

```text
5 6
1 2 4
1 3 1
3 2 2
2 4 5
3 4 8
5 1 1
```

Output:

```text
0 3 1 8 -1
```

Input:

```text
2 1
2 1 7
```

Output:

```text
0 -1
```
