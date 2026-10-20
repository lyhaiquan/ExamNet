# Cái túi 0/1

Có *n* vật, vật thứ *i* nặng *w*ᵢ và có giá trị *v*ᵢ. Túi chịu được tổng khối lượng tối đa *W*. Chọn một số vật (mỗi vật lấy cả hoặc không lấy, không cắt nhỏ) để tổng khối lượng không quá *W* mà tổng giá trị lớn nhất.

Ví dụ *W* = 7, bốn vật (nặng, giá) là (1, 1), (3, 4), (4, 5), (5, 7). Lấy vật 2 và 3: nặng 7, giá 9. Chọn tham lam vật có giá cao nhất (vật 4, giá 7) thì chỉ còn chỗ cho vật 1: tổng 8.

**Bảng `dp`:** `dp[i][c]` là tổng giá trị lớn nhất khi chỉ được chọn trong *i* vật đầu và túi chịu được *c*. Hàng 0 (chưa có vật nào) toàn 0. Với vật thứ *i* (nặng *w*, giá *v*):

- **không lấy** vật *i*: được `dp[i-1][c]`;
- **lấy** vật *i* (chỉ khi *w* ≤ *c*): được `dp[i-1][c-w] + v`; phần còn lại của túi (*c* − *w*) dành cho *i* − 1 vật đầu.

`dp[i][c]` là số lớn hơn trong hai cách. Đáp án là `dp[n][W]`.

Điền xong thì **truy vết** từ ô `dp[n][W]` đi lên: `dp[i][c]` khác `dp[i-1][c]` nghĩa là đã lấy vật *i*, khi đó lùi *c* đi *w*ᵢ.

## Input

Dòng 1 gồm *n* và *W* (1 ≤ *n* ≤ 100, 1 ≤ *W* ≤ 1000). *n* dòng sau, mỗi dòng *w*ᵢ và *v*ᵢ (1 ≤ *w*ᵢ ≤ 1000, 1 ≤ *v*ᵢ ≤ 10⁶).

## Output

Tổng giá trị lớn nhất.

## Ví dụ

Input:

```text
4 7
1 1
3 4
4 5
5 7
```

Output:

```text
9
```
