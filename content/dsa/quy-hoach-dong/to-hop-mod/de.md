# Tổ hợp C(n, k) modulo

Trả lời *q* câu hỏi, mỗi câu hỏi in C(*n*, *k*) — số cách chọn *k* phần tử từ *n* phần tử — chia lấy dư cho 10⁹ + 7.

Gợi ý: **tam giác Pascal**. C(*n*, 0) = C(*n*, *n*) = 1 và C(*n*, *k*) = C(*n* − 1, *k* − 1) + C(*n* − 1, *k*). Tính sẵn cả bảng tới *n* = 1000 một lần, mỗi câu hỏi chỉ việc tra bảng.

## Input

Dòng 1 là *q* (1 ≤ *q* ≤ 10⁵). *q* dòng sau, mỗi dòng hai số *n*, *k* (0 ≤ *k* ≤ *n* ≤ 1000).

## Output

*q* dòng, mỗi dòng C(*n*, *k*) mod 10⁹ + 7.

## Ví dụ

Input:

```text
3
5 2
10 0
1000 500
```

Output:

```text
10
1
159835829
```

Input:

```text
1
0 0
```

Output:

```text
1
```
