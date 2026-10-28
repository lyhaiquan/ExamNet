# Tìm trong dãy đã xoay vòng

Một dãy *n* số nguyên **khác nhau, tăng dần** bị xoay vòng: lấy một đoạn đầu dãy chuyển xuống cuối. Ví dụ `1 3 5 7 9` xoay thành `7 9 1 3 5`.

Cho dãy đã xoay và *q* câu hỏi, mỗi câu hỏi một số *x*. Với mỗi câu hỏi, in vị trí của *x* trong dãy (đánh số từ 0), hoặc −1 nếu *x* không có trong dãy.

Hãy làm mỗi câu hỏi trong O(log *n*) bằng tìm kiếm nhị phân: chia đôi đoạn đang xét thì luôn có một nửa đã tăng dần, kiểm xem *x* có nằm trong nửa đó không.

## Input

Dòng 1 là *n* và *q* (1 ≤ *n*, *q* ≤ 10⁵). Dòng 2 là dãy đã xoay, *n* số nguyên khác nhau có trị tuyệt đối không quá 10⁹. Dòng 3 là *q* số *x* cần tìm.

## Output

*q* dòng, dòng thứ *k* là vị trí của số thứ *k*, hoặc −1.

## Ví dụ

Input:

```text
5 3
7 9 1 3 5
3 7 4
```

Output:

```text
3
0
-1
```

Input:

```text
1 2
8
8 9
```

Output:

```text
0
-1
```
