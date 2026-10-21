import heapq
import sys

data = sys.stdin.buffer.read().split()
n, m = int(data[0]), int(data[1])
ke = [[] for _ in range(n + 1)]
for i in range(m):
    u, v, w = int(data[2 + 3 * i]), int(data[3 + 3 * i]), int(data[4 + 3 * i])
    ke[u].append((v, w))
INF = float("inf")
kc = [INF] * (n + 1)
kc[1] = 0
dong = [(0, 1)]
while dong:
    d, u = heapq.heappop(dong)
    if d > kc[u]:
        continue
    for v, w in ke[u]:
        if d + w < kc[v]:
            kc[v] = d + w
            heapq.heappush(dong, (kc[v], v))
print(*[x if x < INF else -1 for x in kc[1:]])
