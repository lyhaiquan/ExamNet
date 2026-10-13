# Duyệt theo chiều sâu (DFS) bằng ngăn xếp

Cho đồ thị vô hướng *n* đỉnh (đánh số 1 … *n*), *m* cạnh. Duyệt theo chiều sâu bắt đầu từ đỉnh 1: thăm một đỉnh rồi đi **sâu** ngay vào một đỉnh kề chưa thăm, đi tới khi cụt mới lùi lại. Khi có nhiều đỉnh kề chưa thăm, đi vào đỉnh **số nhỏ nhất** trước. In thứ tự thăm.

Đây đúng là thứ tự của cách viết đệ quy quen thuộc:

```python
def dfs(u):
    tham[u] = True
    in u
    for v in các đỉnh kề của u, tăng dần:
        if not tham[v]:
            dfs(v)
```

Nhưng đồ thị có tới 2·10⁴ đỉnh: đi sâu 2·10⁴ tầng thì đệ quy vượt giới hạn của Python. Bài này dùng **ngăn xếp** `st` thay cho ngăn gọi hàm:

1. Đẩy 1 vào `st`.
2. Lặp khi `st` còn phần tử: lấy *u* ra. *u* đã thăm thì bỏ qua (một đỉnh có thể bị đẩy vào nhiều lần). Chưa thì thăm *u*, rồi đẩy các đỉnh kề **chưa thăm** của *u* vào `st` theo thứ tự **giảm dần**: đỉnh nhỏ nhất vào sau cùng, nằm trên đỉnh ngăn xếp, nên được lấy ra thăm trước.

Khung code đã thêm cạnh theo thứ tự để `g.ke(u)` cho các đỉnh kề tăng dần.

## Input

Dòng 1 gồm *n* và *m* (1 ≤ *n* ≤ 2·10⁴, 0 ≤ *m* ≤ 5·10⁴). *m* dòng sau, mỗi dòng hai đỉnh *u* ≠ *v* của một cạnh; không có hai cạnh trùng nhau.

## Output

Các đỉnh theo thứ tự thăm (chỉ những đỉnh đi tới được từ 1), cách nhau một dấu cách.

## Ví dụ

Input:

```text
6 7
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
1 2 4 3 6 5
```
