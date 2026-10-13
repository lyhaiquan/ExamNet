import viz

X = input().strip()
Y = input().strip()
n, m = len(X), len(Y)
# dp[i][j]: độ dài dãy con chung dài nhất của X[:i] và Y[:j]; hai trục ghi sẵn các ký tự của X và Y
dp = viz.Bang(n + 1, m + 1, ten="dp", nhan_hang=" " + X, nhan_cot=" " + Y)

# Điền dp[i][j] với i từ 1 tới n, j từ 1 tới m.

print(dp[n][m])
