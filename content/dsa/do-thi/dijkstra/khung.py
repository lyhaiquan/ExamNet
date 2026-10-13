import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_trong_so=True, ten="g")  # đỉnh 0..n-1; for v, w in g.ke(u) cho đỉnh kề và độ dài
for _ in range(m):
    u, v, w = map(int, input().split())
    g.them_canh(u, v, w)
d = viz.Mang([-1] * n, ten="d")  # d[v]: đường ngắn nhất đã tìm được từ 0 tới v; -1 là chưa tìm được
chot = [False] * n               # chot[v]: d[v] đã chắc chắn ngắn nhất
d[0] = 0

# Lặp:
#   u = đỉnh chưa chốt, d[u] != -1, d[u] nhỏ nhất (không còn thì dừng); chot[u] = True
#   (g.danh_dau(u, "xong") để tô đỉnh đã chốt)
#   với mỗi (v, w) trong g.ke(u), v chưa chốt: d[v] == -1 hoặc d[u] + w < d[v] thì d[v] = d[u] + w
#   (g.nhan(v, d[v]) để ghi d dưới đỉnh)

print(*d)
