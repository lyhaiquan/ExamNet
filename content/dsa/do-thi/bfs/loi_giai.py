import viz

n, m = map(int, input().split())
g = viz.DoThi(n, ten="g", bat_dau=1)
for _ in range(m):
    u, v = map(int, input().split())
    g.them_canh(u, v)

d = [-1] * (n + 1)
q = viz.HangDoi(ten="q")
d[1] = 0
g.nhan(1, 0)
q.enq(1)
viz.key("Bắt đầu từ đỉnh 1, khoảng cách 0")
while q:
    u = q.deq()
    viz.key(f"Lấy {u} ra khỏi hàng đợi, xét các đỉnh kề")
    for v in g.ke(u):
        if d[v] == -1:
            viz.say(f"{v} chưa tới: d[{v}] = d[{u}] + 1 = {d[u] + 1}, vào hàng đợi")
            d[v] = d[u] + 1
            g.nhan(v, d[v])
            g.danh_dau_canh(u, v, "xong")
            q.enq(v)
        else:
            viz.say(f"{v} đã có khoảng cách, bỏ qua")
    g.danh_dau(u, "xong")
print(*d[1:])
