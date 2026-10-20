import viz

n, W = map(int, input().split())
vat = [tuple(map(int, input().split())) for _ in range(n)]
dp = viz.Bang(n + 1, W + 1, ten="dp")

for i in range(1, n + 1):
    w, v = vat[i - 1]
    viz.key(f"Vật {i}: nặng {w}, giá {v}. Hàng {i}: được chọn trong {i} vật đầu")
    for c in range(W + 1):
        khong = dp[i - 1][c]
        if w <= c:
            lay = dp[i - 1][c - w] + v
            if lay > khong:
                viz.say(f"Túi {c}: lấy vật {i} được {lay} (giá {v} + dp[{i - 1}][{c - w}]), hơn không lấy ({khong})")
            dp[i][c] = max(khong, lay)
        else:
            dp[i][c] = khong

# Truy vết ngược từ ô cuối. Đọc bảng bằng trang_thai() để không sinh thêm sự kiện đọc làm rối hình.
o = dp.trang_thai()["o"]
viz.key(f"Truy vết từ dp[{n}][{W}]: ô khác ô ngay trên nghĩa là đã lấy vật của hàng đó")
c, lay = W, []
dp.danh_dau(n, c)
for i in range(n, 0, -1):
    if o[i][c] != o[i - 1][c]:
        lay.append(i)
        c -= vat[i - 1][0]
    dp.danh_dau(i - 1, c)
viz.key(f"Lấy các vật {', '.join(map(str, reversed(lay))) or '(không vật nào)'}: tổng giá {o[n][W]}")
print(dp[n][W])
