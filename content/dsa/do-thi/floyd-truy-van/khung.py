n, m, q = map(int, input().split())
INF = float("inf")
d = [[0 if i == j else INF for j in range(n + 1)] for i in range(n + 1)]
for _ in range(m):
    u, v, w = map(int, input().split())
    d[u][v] = min(d[u][v], w)
# Floyd: for k ... for i ... for j ...
