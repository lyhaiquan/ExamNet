import viz

n, S = map(int, input().split())
a = viz.Mang(sorted(map(int, input().split())), ten="a")  # đã sắp tăng dần
# con_lai[i] = a[i] + a[i+1] + … + a[n-1] (tổng hậu tố), con_lai[n] = 0
con_lai = [0] * (n + 1)
for i in range(n - 1, -1, -1):
    con_lai[i] = con_lai[i + 1] + a[i]


def thu(i, tong):
    # tong == S: được một cách, trả 1
    # Cắt nhánh (gọi viz.cat("…") rồi trả 0):
    #   tong + con_lai[i] < S  → lấy hết số còn lại vẫn chưa đủ
    #   tong + a[i] > S        → lấy a[i] (và mọi số sau nó) đều vượt S
    # Còn lại: số cách khi lấy a[i] + số cách khi bỏ a[i]
    return 0


print(thu(0, 0))
