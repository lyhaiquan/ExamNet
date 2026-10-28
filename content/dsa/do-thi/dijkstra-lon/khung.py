import heapq

n, m = map(int, input().split())
ke = [[] for _ in range(n + 1)]
for _ in range(m):
    u, v, w = map(int, input().split())
    ke[u].append((v, w))
INF = float("inf")
kc = [INF] * (n + 1)
kc[1] = 0
dong = [(0, 1)]
