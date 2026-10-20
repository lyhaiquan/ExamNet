import viz

n, S = map(int, input().split())
xu = list(map(int, input().split()))
dp = viz.Mang([-1] * (S + 1), ten="dp")  # dp[s]: số đồng ít nhất để đổi s; -1 là không đổi được
dp[0] = 0

# Với s từ 1 tới S (dp.con_tro("s", s) để thấy con trỏ), với mỗi mệnh giá x <= s:
#   dp[s - x] khác -1 và (dp[s] == -1 hoặc dp[s - x] + 1 < dp[s]) thì dp[s] = dp[s - x] + 1

print(dp[S])
