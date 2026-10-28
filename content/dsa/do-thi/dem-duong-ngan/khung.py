from collections import deque

MOD = 10**9 + 7
n, m, s, t = map(int, input().split())
ke = [[] for _ in range(n + 1)]
for _ in range(m):
    u, v = map(int, input().split())
    ke[u].append(v)
    ke[v].append(u)
kc = [-1] * (n + 1)
so = [0] * (n + 1)
