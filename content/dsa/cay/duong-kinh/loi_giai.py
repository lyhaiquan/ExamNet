from collections import deque
import sys

data = sys.stdin.read().split()
n = int(data[0])
ke = [[] for _ in range(n + 1)]
for i in range(n - 1):
    u, v = int(data[1 + 2 * i]), int(data[2 + 2 * i])
    ke[u].append(v)
    ke[v].append(u)


def bfs(nguon):
    kc = [-1] * (n + 1)
    kc[nguon] = 0
    q = deque([nguon])
    while q:
        u = q.popleft()
        for v in ke[u]:
            if kc[v] < 0:
                kc[v] = kc[u] + 1
                q.append(v)
    xa = max(range(1, n + 1), key=kc.__getitem__)
    return xa, kc[xa]


u, _ = bfs(1)
print(bfs(u)[1])
