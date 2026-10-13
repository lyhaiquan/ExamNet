# Tìm kiếm tuần tự

Cho dãy *a*₀, *a*₁, …, *a*ₙ₋₁ (chưa sắp) và số *x*. Tìm **vị trí đầu tiên** *i* mà `a[i] == x`. Không có thì in −1.

**Tìm kiếm tuần tự:** so *x* lần lượt với `a[0]`, `a[1]`, … ; gặp thì dừng ngay và trả vị trí đó. Đi hết dãy mà không gặp thì trả −1. Tệ nhất phải so cả *n* phần tử: O(*n*). Dãy chưa sắp thì không có cách nào nhanh hơn, vì phần tử chưa xem có thể là bất kỳ số nào.

Vị trí tính từ 0. Không dùng `index`, `count`, `find`.

## Input

Dòng 1 gồm *n* và *x* (1 ≤ *n* ≤ 10⁵, |*x*| ≤ 10⁹). Dòng 2 gồm *n* số nguyên, trị tuyệt đối ≤ 10⁹.

## Output

Vị trí đầu tiên của *x*, hoặc −1.

## Ví dụ

| Input | Output |
| --- | --- |
| `6 9`<br>`4 7 9 1 9 3` | `2` |
| `3 5`<br>`1 2 3` | `-1` |
