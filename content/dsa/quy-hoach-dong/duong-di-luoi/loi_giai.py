import viz

m, n = map(int, input().split())
a = viz.Bang.tu([list(map(int, input().split())) for _ in range(m)], ten="a")
dp = viz.Bang(m, n, ten="dp")
viz.key("dp[i][j]: tổng nhỏ nhất từ ô (0, 0) tới ô (i, j); điền từng hàng, trái sang phải")

for i in range(m):
    for j in range(n):
        if i == 0 and j == 0:
            dp[0][0] = a[0][0]
        elif i == 0:
            dp[i][j] = dp[i][j - 1] + a[i][j]
        elif j == 0:
            dp[i][j] = dp[i - 1][j] + a[i][j]
        else:
            tren, trai = dp[i - 1][j], dp[i][j - 1]
            if j == 1 or tren != trai:
                viz.say(f"Ô ({i}, {j}): đến từ {'trên' if tren <= trai else 'trái'} rẻ hơn ({min(tren, trai)})")
            dp[i][j] = min(tren, trai) + a[i][j]
    viz.key(f"Xong hàng {i}")

# Truy vết ngược từ ô cuối, đọc bảng bằng trang_thai() để không sinh thêm sự kiện đọc
d = dp.trang_thai()["o"]
i, j = m - 1, n - 1
viz.key("Truy vết từ ô cuối: lùi về ô trên hoặc ô trái, ô nào có dp nhỏ hơn")
while True:
    a.danh_dau(i, j)
    dp.danh_dau(i, j)
    if i == 0 and j == 0:
        break
    if j == 0 or (i > 0 and d[i - 1][j] <= d[i][j - 1]):
        i -= 1
    else:
        j -= 1
viz.key(f"Đường đi tô xanh trên lưới a, tổng {d[m - 1][n - 1]}")
print(dp[m - 1][n - 1])
