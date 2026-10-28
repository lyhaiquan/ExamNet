# Duyệt cây zic zắc

Duyệt cây nhị phân **theo tầng**, nhưng đổi chiều ở mỗi tầng: tầng của gốc đọc từ trái sang phải, tầng kế tiếp từ phải sang trái, rồi lại từ trái sang phải, và cứ thế.

Gợi ý: BFS lấy từng tầng như bình thường (từ trái sang phải), rồi đảo ngược danh sách khoá của các tầng lẻ trước khi in.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵), số nút. *n* dòng sau, dòng thứ *i* gồm ba số `khoa trai phai`: khoá của nút *i* (|khoá| ≤ 10⁹), số thứ tự con trái và con phải của nút *i* (0 nếu không có). Nút 1 là gốc.

## Output

Mỗi tầng một dòng: khoá các nút của tầng theo thứ tự zic zắc.

## Ví dụ

Input:

```text
7
1 2 3
2 4 5
3 6 7
4 0 0
5 0 0
6 0 0
7 0 0
```

Output:

```text
1
3 2
4 5 6 7
```

Input:

```text
3
10 0 2
20 3 0
30 0 0
```

Output:

```text
10
20
30
```
