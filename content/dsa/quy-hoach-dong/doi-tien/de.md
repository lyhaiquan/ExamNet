# Đổi tiền ít đồng nhất

Có *n* loại đồng xu với mệnh giá *x*₁, …, *x*ₙ, mỗi loại có không giới hạn số đồng. Cần đổi đúng số tiền *S* bằng ít đồng nhất. Không đổi được thì in −1.

Ví dụ mệnh giá 1, 3, 4 và *S* = 6. Chọn **tham lam** đồng lớn nhất trước: 4 + 1 + 1, ba đồng. Nhưng 3 + 3 chỉ hai đồng. Tham lam không phải lúc nào cũng đúng; quy hoạch động thì luôn đúng.

**Mảng `dp`:** `dp[s]` là số đồng ít nhất để đổi số tiền *s*, −1 nếu không đổi được. `dp[0] = 0` (không cần đồng nào). Với *s* từ 1 tới *S*: đồng **cuối cùng** dùng để đổi *s* là một mệnh giá *x* ≤ *s* nào đó, phần còn lại *s* − *x* phải đổi ít đồng nhất:

`dp[s] = 1 + min(dp[s - x])` với mọi mệnh giá *x* ≤ *s* mà `dp[s - x]` khác −1 (không có *x* nào như vậy thì `dp[s] = -1`).

Đáp án là `dp[S]`. Dùng −1 (không dùng vô cực) cho "không đổi được" để mảng vẽ được thành cột.

## Input

Dòng 1 gồm *n* và *S* (1 ≤ *n* ≤ 10, 1 ≤ *S* ≤ 10⁴). Dòng 2 gồm *n* mệnh giá (1 ≤ *x*ᵢ ≤ 10⁴).

## Output

Số đồng ít nhất, hoặc −1.

## Ví dụ

| Input | Output |
| --- | --- |
| `3 6`<br>`1 3 4` | `2` |
| `2 7`<br>`2 4` | `-1` |
| `3 11`<br>`1 2 5` | `3` |
