from collections import deque

n = int(input())
x1, y1, x2, y2 = map(int, input().split())
NUOC = [(1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2)]
kc = [[-1] * (n + 1) for _ in range(n + 1)]
kc[x1][y1] = 0
q = deque([(x1, y1)])
while q and kc[x2][y2] < 0:
    x, y = q.popleft()
    for a, b in NUOC:
        u, v = x + a, y + b
        if 1 <= u <= n and 1 <= v <= n and kc[u][v] < 0:
            kc[u][v] = kc[x][y] + 1
            q.append((u, v))
print(kc[x2][y2])
