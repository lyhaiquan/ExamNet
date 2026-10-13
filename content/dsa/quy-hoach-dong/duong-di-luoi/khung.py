import viz

m, n = map(int, input().split())
a = viz.Bang.tu([list(map(int, input().split())) for _ in range(m)], ten="a")
dp = viz.Bang(m, n, ten="dp")  # dp[i][j]: tổng nhỏ nhất của đường đi từ (0, 0) tới (i, j)

# Điền từng hàng từ trên xuống, mỗi hàng từ trái sang phải:
#   dp[0][0] = a[0][0]; hàng 0 chỉ từ bên trái; cột 0 chỉ từ bên trên;
#   ô khác: dp[i][j] = min(dp[i - 1][j], dp[i][j - 1]) + a[i][j]

print(dp[m - 1][n - 1])
