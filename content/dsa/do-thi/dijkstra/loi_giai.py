import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_trong_so=True, ten="g")
for _ in range(m):
    u, v, w = map(int, input().split())
    g.them_canh(u, v, w)
d = viz.Mang([-1] * n, ten="d")
chot = [False] * n
cha = [-1] * n  # cha[v]: đỉnh đứng trước v trên đường ngắn nhất đã tìm được
d[0] = 0
g.nhan(0, 0)
for v in range(1, n):
    g.nhan(v, "∞")
viz.key("d[0] = 0, các đỉnh khác chưa có đường (∞, trong mảng ghi -1)")

while True:
    u = -1
    for v in range(n):
        if not chot[v] and d[v] != -1 and (u == -1 or d[v] < d[u]):
            u = v
    if u == -1:
        break
    chot[u] = True
    d.con_tro("u", u)
    viz.key(f"Chốt đỉnh {u}: d[{u}] = {d[u]} nhỏ nhất trong các đỉnh chưa chốt")
    g.danh_dau(u, "xong")
    if cha[u] != -1:
        g.danh_dau_canh(cha[u], u, "xong")
    for v, w in g.ke(u):
        if chot[v]:
            continue
        moi, cu = d[u] + w, d[v]
        if cu == -1 or moi < cu:
            viz.say(f"Qua {u} tới {v}: {d[u]} + {w} = {moi}" + (f", ngắn hơn {cu}" if cu != -1 else ""))
            d[v] = moi
            cha[v] = u
            g.nhan(v, moi)
        else:
            viz.say(f"Qua {u} tới {v}: {d[u]} + {w} = {moi}, không ngắn hơn {cu}")
viz.key("Không còn đỉnh chưa chốt đi tới được: xong. Cạnh tô xanh là cây đường đi ngắn nhất")
print(*d)
