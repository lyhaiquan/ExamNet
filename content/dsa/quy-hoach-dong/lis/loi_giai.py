import viz

n = int(input())
a = viz.Mang(list(map(int, input().split())), ten="a")
dp = viz.Mang([1] * n, ten="dp")
truoc = [-1] * n  # truoc[i]: phần tử đứng trước a[i] trong dãy dài nhất kết thúc tại a[i], để truy vết
viz.key("dp[i]: dãy con tăng dài nhất kết thúc tại a[i]; ban đầu mỗi phần tử tự là một dãy độ dài 1")

for i in range(n):
    a.con_tro("i", i)
    for j in range(i):
        a.con_tro("j", j)
        if a[j] < a[i] and dp[j] + 1 > dp[i]:
            viz.say(f"a[{j}] < a[{i}]: nối a[{i}] vào sau dãy kết thúc tại a[{j}], dài {dp[j] + 1}")
            dp[i] = dp[j] + 1
            truoc[i] = j
    viz.key(f"dp[{i}] = {dp[i]}")

# truy vết bằng bản sao thường của dp, a để không sinh thêm sự kiện đọc
d, x = list(dp), list(a)
k = d.index(max(d))
day = []
while k != -1:
    day.append(x[k])
    k = truoc[k]
viz.key(f"Lớn nhất trong dp là {max(d)}: một dãy con tăng dài nhất là {' '.join(map(str, reversed(day)))}")
print(max(d))
