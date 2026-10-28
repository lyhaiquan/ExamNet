# Phân tích thành tổng

Liệt kê mọi cách viết số nguyên dương *n* thành tổng các số nguyên dương. Hai cách chỉ khác thứ tự các số hạng coi là một: mỗi cách viết các số hạng theo thứ tự **không tăng**.

Các cách xếp theo thứ tự từ điển **giảm dần**: cách có số hạng đầu lớn hơn đứng trước; bằng nhau thì so số hạng thứ hai, và cứ thế. Ví dụ với *n* = 4: `4`, `3 1`, `2 2`, `2 1 1`, `1 1 1 1`.

Gợi ý: **quay lui** chọn từng số hạng, số hạng sau không lớn hơn số hạng trước; thử số hạng từ lớn tới nhỏ.

## Input

Một dòng *n* (1 ≤ *n* ≤ 30).

## Output

Dòng đầu là số cách. Mỗi dòng sau là một cách, các số hạng cách nhau một dấu cách.

## Ví dụ

Input:

```text
4
```

Output:

```text
5
4
3 1
2 2
2 1 1
1 1 1 1
```

Input:

```text
1
```

Output:

```text
1
1
```
