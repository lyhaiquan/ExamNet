from collections import deque
import sys

MOD = 10**9 + 7
data = sys.stdin.buffer.read().split()
n, m, s, t = map(int, data[:4])
ke = [[] for _ in range(n + 1)]
for i in range(m):
    u, v = int(data[4 + 2 * i]), int(data[5 + 2 * i])
    ke[u].append(v)
    ke[v].append(u)
kc = [-1] * (n + 1)
so = [0] * (n + 1)
kc[s], so[s] = 0, 1
q = deque([s])
while q:
    u = q.popleft()
    for v in ke[u]:
        if kc[v] < 0:
            kc[v] = kc[u] + 1
            so[v] = so[u]
            q.append(v)
        elif kc[v] == kc[u] + 1:
            so[v] = (so[v] + so[u]) % MOD
print(kc[t], so[t])
