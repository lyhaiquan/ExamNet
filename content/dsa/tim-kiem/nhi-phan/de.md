# Tìm kiếm nhị phân

Cho dãy *a* gồm *n* số nguyên **tăng ngặt** và *q* câu hỏi. Mỗi câu hỏi là một số *x*: in vị trí *i* mà `a[i] == x` (tính từ 0), hoặc −1 nếu không có.

**Tìm kiếm nhị phân:** giữ đoạn `[lo, hi]` có thể chứa *x*, ban đầu `lo = 0`, `hi = n - 1`. Khi `lo <= hi`:

- `mid = (lo + hi) // 2`;
- `a[mid] == x`: tìm thấy;
- `a[mid] < x`: mọi số từ `a[lo]` tới `a[mid]` đều nhỏ hơn *x* (dãy tăng), bỏ nửa trái: `lo = mid + 1`;
- `a[mid] > x`: bỏ nửa phải: `hi = mid - 1`.

Đoạn rỗng (`lo > hi`) thì không có *x*. Mỗi bước bỏ đi một nửa: không quá ⌊log₂ *n*⌋ + 1 bước. Với *n* = 10⁵ và *q* = 2·10⁴, tìm tuần tự cần tới 10⁹ phép so, quá giờ; nhị phân chỉ khoảng 3,4·10⁵.

## Input

Dòng 1 gồm *n* và *q* (1 ≤ *n* ≤ 10⁵, 1 ≤ *q* ≤ 2·10⁴). Dòng 2 gồm *n* số tăng ngặt, trị tuyệt đối ≤ 10⁹. Dòng 3 gồm *q* số *x*.

## Output

*q* dòng, mỗi dòng là câu trả lời.

## Ví dụ

Input:

```text
10 2
2 5 8 12 16 23 38 56 72 91
23 40
```

Output:

```text
5
-1
```
