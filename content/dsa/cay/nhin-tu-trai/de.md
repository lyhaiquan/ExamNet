# Nhìn cây từ bên trái

Đứng bên trái cây nhị phân nhìn sang, ở mỗi tầng ta chỉ thấy nút **đầu tiên từ trái sang** của tầng đó. In khoá của các nút nhìn thấy, từ tầng của gốc trở xuống.

Gợi ý: duyệt cây **theo tầng** bằng hàng đợi (BFS), mỗi lần lấy ra đủ một tầng; nút đầu tiên của tầng là nút nhìn thấy.

## Input

Dòng 1 là *n* (1 ≤ *n* ≤ 10⁵), số nút. *n* dòng sau, dòng thứ *i* gồm ba số `khoa trai phai`: khoá của nút *i* (|khoá| ≤ 10⁹), số thứ tự con trái và con phải của nút *i* (0 nếu không có). Nút 1 là gốc.

## Output

Một dòng: khoá của các nút nhìn thấy, theo thứ tự từ tầng trên xuống.

## Ví dụ

Input:

```text
6
1 2 3
2 0 4
3 5 0
4 0 0
5 0 6
6 0 0
```

Output:

```text
1 2 4 6
```

Input:

```text
1
42 0 0
```

Output:

```text
42
```
