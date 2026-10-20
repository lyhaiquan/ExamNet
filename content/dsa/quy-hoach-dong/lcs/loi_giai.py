import viz

X = input().strip()
Y = input().strip()
n, m = len(X), len(Y)
dp = viz.Bang(n + 1, m + 1, ten="dp", nhan_hang=" " + X, nhan_cot=" " + Y)

for i in range(1, n + 1):
    for j in range(1, m + 1):
        if X[i - 1] == Y[j - 1]:
            viz.say(f"X[{i - 1}] = Y[{j - 1}] = '{X[i - 1]}': lấy ô chéo trên trái cộng 1")
            dp[i][j] = dp[i - 1][j - 1] + 1
        else:
            dp[i][j] = max(dp[i - 1][j], dp[i][j - 1])
    viz.key(f"Xong hàng {i} (ký tự '{X[i - 1]}' của X)")

# Truy vết ngược từ ô cuối để tìm một dãy con chung dài nhất. Đọc bảng bằng trang_thai() để không sinh
# thêm sự kiện đọc làm rối hình; các ô trên đường đi được tô xanh lá.
bang = dp.trang_thai()["o"]
i, j, chung = n, m, []
viz.say("Truy vết từ ô cuối: hai ký tự bằng nhau thì đi chéo lên, không thì sang ô lớn hơn")
dp.danh_dau(i, j)
while i > 0 and j > 0:
    if X[i - 1] == Y[j - 1]:
        chung.append(X[i - 1])
        i, j = i - 1, j - 1
    elif bang[i - 1][j] >= bang[i][j - 1]:
        i -= 1
    else:
        j -= 1
    dp.danh_dau(i, j)
viz.key(f"Truy vết xong: một dãy con chung dài nhất là {''.join(reversed(chung))}")

print(dp[n][m])
