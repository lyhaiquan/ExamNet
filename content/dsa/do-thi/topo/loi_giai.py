import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_huong=True, ten="g", bat_dau=1)
for _ in range(m):
    u, v = map(int, input().split())
    g.them_canh(u, v)
vao = [0] * (n + 1)
for u in g.dinh():
    for v in g.ke(u):
        vao[v] += 1
for v in g.dinh():
    g.nhan(v, vao[v])
viz.key("Bậc vào (số môn phải học trước) ghi dưới mỗi đỉnh")
q = viz.HangDoi(ten="q")
for v in g.dinh():
    if vao[v] == 0:
        q.enq(v)
viz.say("Các đỉnh bậc vào 0 vào hàng đợi: học được ngay")
kq = []
while q:
    u = q.deq()
    kq.append(u)
    g.danh_dau(u, "xong")
    viz.key(f"Lấy {u} ra: xếp thứ {len(kq)}; các môn sau {u} bớt một môn phải chờ")
    for v in g.ke(u):
        vao[v] -= 1
        g.nhan(v, vao[v])
        g.danh_dau_canh(u, v, "xong")
        if vao[v] == 0:
            viz.say(f"Bậc vào của {v} về 0: vào cuối hàng")
            q.enq(v)
if len(kq) == n:
    viz.key(f"Đã xếp đủ {n} đỉnh")
    print(*kq)
else:
    con = [v for v in g.dinh() if vao[v] > 0]
    for v in con:
        g.danh_dau(v, "loai")
    viz.key(f"Hàng rỗng mà mới xếp {len(kq)}/{n} đỉnh: các đỉnh tô đỏ nằm trên vòng, không xếp được")
    print(-1)
