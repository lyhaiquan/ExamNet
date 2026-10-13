import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_trong_so=True, ten="g")
for _ in range(m):
    u, v, w = map(int, input().split())
    g.them_canh(u, v, w)
kc = viz.Mang([-1] * n, ten="kc")
trong_cay = [False] * n
noi = [-1] * n  # noi[v]: đỉnh trong cây ở đầu kia cạnh nhẹ nhất của v, để tô cạnh cây khung
kc[0] = 0
tong = 0
viz.key("Cây bắt đầu từ đỉnh 0; kc[v] là cạnh nhẹ nhất nối v với cây (-1: chưa có)")
for _ in range(n):
    u = -1
    for v in range(n):
        if not trong_cay[v] and kc[v] != -1 and (u == -1 or kc[v] < kc[u]):
            u = v
    trong_cay[u] = True
    tong += kc[u]
    kc.con_tro("u", u)
    g.danh_dau(u, "xong")
    if noi[u] != -1:
        g.danh_dau_canh(noi[u], u, "xong")
        viz.key(f"Thêm đỉnh {u} vào cây bằng cạnh {noi[u]}–{u} nặng {kc[u]}: tổng {tong}")
    for v, w in g.ke(u):
        if trong_cay[v]:
            continue
        if kc[v] == -1 or w < kc[v]:
            viz.say(f"Cạnh {u}–{v} nặng {w} nhẹ hơn cạnh đang nối {v} với cây: kc[{v}] = {w}")
            kc[v] = w
            noi[v] = u
            g.nhan(v, w)
viz.key(f"Đủ {n} đỉnh: tổng trọng số cây khung nhỏ nhất là {tong}")
print(tong)
