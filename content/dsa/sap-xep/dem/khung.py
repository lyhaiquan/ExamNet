import viz

a = viz.Mang(list(map(int, input().split())), ten="a")
n = len(a)
k = max(a)
dem = viz.Mang([0] * (k + 1), ten="dem")  # dem[v]: số lần v xuất hiện trong a
kq = viz.Mang([0] * n, ten="kq")          # dãy đã sắp

# Bước 1: với i từ 0 tới n - 1: dem[a[i]] += 1
# Bước 2: vt = 0; với v từ 0 tới k: ghi dem[v] lần số v vào kq[vt], kq[vt + 1], …

print(*kq)
