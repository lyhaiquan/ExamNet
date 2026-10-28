# Tổ hợp có tổng S

Cho *n* số nguyên dương **khác nhau** và số *S*. Liệt kê mọi cách chọn các số (mỗi số được dùng **nhiều lần**) để tổng bằng *S*. Mỗi cách viết các số theo thứ tự không giảm; các cách xếp theo thứ tự từ điển tăng dần.

Gợi ý: sắp xếp các số rồi **quay lui**: số chọn sau không nhỏ hơn số chọn trước; tổng đã vượt *S* thì dừng nhánh đó (cắt nhánh).

## Input

Dòng 1 là *n* và *S* (1 ≤ *n* ≤ 10, 1 ≤ *S* ≤ 40). Dòng 2 là *n* số nguyên dương khác nhau, mỗi số từ 2 tới 40.

## Output

Dòng đầu là số cách *c*. *c* dòng sau, mỗi dòng một cách, các số cách nhau một dấu cách. Không có cách nào thì chỉ in `0`.

## Ví dụ

Input:

```text
4 7
2 3 6 7
```

Output:

```text
2
2 2 3
7
```

Input:

```text
2 1
2 3
```

Output:

```text
0
```
