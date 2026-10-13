import viz

INF = float("inf")
n, m = map(int, input().split())
d = viz.Bang(n, n, INF, ten="d")  # d[i][j]: đường ngắn nhất đã biết từ i tới j; bảng hiện vô cực là ∞
for i in range(n):
    d[i][i] = 0
for _ in range(m):
    u, v, w = map(int, input().split())
    if w < d[u][v]:
        d[u][v] = w

# Với k từ 0 tới n - 1 (vòng ngoài cùng), với mọi i, j:
#   d[i][k] + d[k][j] < d[i][j] thì d[i][j] = d[i][k] + d[k][j]

for i in range(n):
    print(*(-1 if d[i][j] == INF else d[i][j] for j in range(n)))
