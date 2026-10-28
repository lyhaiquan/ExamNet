# Đường kính của cây

Cho cây (đồ thị vô hướng liên thông, không có chu trình) gồm *n* đỉnh. **Đường kính** của cây là số cạnh trên đường đi dài nhất giữa hai đỉnh.

Gợi ý: BFS từ một đỉnh bất kỳ tới đỉnh xa nhất *u*; BFS lần nữa từ *u*, khoảng cách xa nhất tìm được là đường kính. Dùng BFS (hoặc DFS bằng ngăn xếp) thay cho đệ quy: cây có thể là một đường dài 10⁵ đỉnh.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵). *n* − 1 dòng sau, mỗi dòng hai đỉnh *u*, *v* của một cạnh (1 ≤ *u*, *v* ≤ *n*).

## Output

Một số: đường kính của cây.

## Ví dụ

Input:

```text
5
1 2
1 3
3 4
3 5
```

Output:

```text
3
```

Input:

```text
1
```

Output:

```text
0
```
