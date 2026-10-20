import viz

n, m = map(int, input().split())
g = viz.DoThi(n, co_huong=True, ten="g", bat_dau=1)
for _ in range(m):
    u, v = map(int, input().split())
    g.them_canh(u, v)
vao = [0] * (n + 1)  # bậc vào
for u in g.dinh():
    for v in g.ke(u):
        vao[v] += 1
q = viz.HangDoi(ten="q")
kq = []
# Cho các đỉnh bậc vào 0 vào q theo thứ tự tăng dần (g.nhan(v, vao[v]) để ghi bậc vào dưới đỉnh)
# Khi q còn: u = q.deq(), kq.append(u); với v trong g.ke(u): vao[v] -= 1, về 0 thì q.enq(v)
print(*kq) if len(kq) == n else print(-1)
