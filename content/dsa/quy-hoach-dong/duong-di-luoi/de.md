# Đường đi tổng nhỏ nhất trên lưới

Lưới *m* × *n*, mỗi ô ghi một số không âm. Đi từ ô trên cùng bên trái (0, 0) tới ô dưới cùng bên phải (*m* − 1, *n* − 1), mỗi bước chỉ sang **phải** hoặc **xuống**. Tìm tổng nhỏ nhất của các số trên đường đi (tính cả ô đầu và ô cuối).

**Bảng `dp`:** `dp[i][j]` là tổng nhỏ nhất của đường đi từ (0, 0) tới ô (*i*, *j*). Muốn tới (*i*, *j*) thì bước cuối đến từ ô **trên** (*i* − 1, *j*) hoặc ô **trái** (*i*, *j* − 1), nên:

- `dp[0][0] = a[0][0]`;
- hàng 0 chỉ đến được từ bên trái: `dp[0][j] = dp[0][j-1] + a[0][j]`;
- cột 0 chỉ đến được từ bên trên: `dp[i][0] = dp[i-1][0] + a[i][0]`;
- các ô khác: `dp[i][j] = min(dp[i-1][j], dp[i][j-1]) + a[i][j]`.

Điền theo từng hàng từ trên xuống, mỗi hàng từ trái sang phải: khi tới ô (*i*, *j*) thì ô trên và ô trái đã có. Đáp án là `dp[m-1][n-1]`.

## Input

Dòng 1 gồm *m* và *n* (1 ≤ *m*, *n* ≤ 100). *m* dòng sau, mỗi dòng *n* số từ 0 tới 9.

## Output

Tổng nhỏ nhất.

## Ví dụ

Input:

```text
3 4
1 3 1 2
1 5 1 9
4 2 1 1
```

Output:

```text
8
```

Đường đi 1 → 3 → 1 → 1 → 1 → 1 (phải, phải, xuống, xuống, phải).
