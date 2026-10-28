import sys

data = sys.stdin.buffer.read().split()
n, m, q = int(data[0]), int(data[1]), int(data[2])
INF = float("inf")
d = [[0 if i == j else INF for j in range(n + 1)] for i in range(n + 1)]
p = 3
for _ in range(m):
    u, v, w = int(data[p]), int(data[p + 1]), int(data[p + 2])
    p += 3
    if w < d[u][v]:
        d[u][v] = w
for k in range(1, n + 1):
    dk = d[k]
    for i in range(1, n + 1):
        dik = d[i][k]
        if dik < INF:
            d[i] = [a if a <= dik + b else dik + b for a, b in zip(d[i], dk)]
kq = []
for _ in range(q):
    x = d[int(data[p])][int(data[p + 1])]
    p += 2
    kq.append(str(x) if x < INF else "-1")
print("\n".join(kq))
