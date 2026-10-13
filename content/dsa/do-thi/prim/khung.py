import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_trong_so=True, ten="g")  # for v, w in g.ke(u): đỉnh kề và trọng số
for _ in range(m):
    u, v, w = map(int, input().split())
    g.them_canh(u, v, w)
kc = viz.Mang([-1] * n, ten="kc")  # kc[v]: cạnh nhẹ nhất nối v với cây; -1 là chưa có
trong_cay = [False] * n
kc[0] = 0
tong = 0
# Lặp n lần:
#   u = đỉnh chưa trong cây, kc[u] != -1, kc[u] nhỏ nhất; trong_cay[u] = True; tong += kc[u]
#   (g.danh_dau(u, "xong") để tô đỉnh đã vào cây)
#   với (v, w) trong g.ke(u), v chưa trong cây: kc[v] == -1 hoặc w < kc[v] thì kc[v] = w
print(tong)
