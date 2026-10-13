import viz

n = int(input())
a = viz.Mang(list(map(int, input().split())), ten="a")
dp = viz.Mang([1] * n, ten="dp")  # dp[i]: độ dài dãy con tăng dài nhất kết thúc tại a[i]

# Với i từ 0 tới n - 1, xét mọi j < i: a[j] < a[i] và dp[j] + 1 > dp[i] thì dp[i] = dp[j] + 1
# (a.con_tro("i", i), a.con_tro("j", j) để thấy hai con trỏ trên hình)

print(max(dp))
