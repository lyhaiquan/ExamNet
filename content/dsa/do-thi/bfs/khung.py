import viz

n, m = map(int, input().split())
g = viz.DoThi(n, ten="g", bat_dau=1)
for _ in range(m):
    u, v = map(int, input().split())
    g.them_canh(u, v)

d = [-1] * (n + 1)
q = viz.HangDoi(ten="q")
# BFS từ đỉnh 1: d[1] = 0, q.enq(1).
# Khi q còn phần tử: u = q.deq(); với mỗi v trong g.ke(u):
#     nếu d[v] == -1 thì d[v] = d[u] + 1 và q.enq(v)
# Muốn thấy khoảng cách trên hình: g.nhan(v, d[v]) sau khi gán d[v].

print(*d[1:])
