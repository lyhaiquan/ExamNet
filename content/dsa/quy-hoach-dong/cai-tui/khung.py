import viz

n, W = map(int, input().split())
vat = [tuple(map(int, input().split())) for _ in range(n)]  # vat[i - 1] = (nặng, giá) của vật thứ i
# dp[i][c]: giá trị lớn nhất khi chọn trong i vật đầu, túi chịu được c; hàng 0 toàn 0
dp = viz.Bang(n + 1, W + 1, ten="dp")

# Điền dp[i][c] với i từ 1 tới n, c từ 0 tới W:
#   không lấy vật i: dp[i - 1][c];  lấy (nếu nặng w <= c): dp[i - 1][c - w] + giá

print(dp[n][W])
