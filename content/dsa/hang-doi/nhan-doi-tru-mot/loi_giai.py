from collections import deque
import sys

data = sys.stdin.read().split()
t = int(data[0])
kq = []
for i in range(t):
    S, T = int(data[1 + 2 * i]), int(data[2 + 2 * i])
    tran = 2 * max(S, T)
    kc = [-1] * (tran + 1)
    kc[S] = 0
    q = deque([S])
    while kc[T] < 0:
        x = q.popleft()
        for y in (2 * x, x - 1):
            if 1 <= y <= tran and kc[y] < 0:
                kc[y] = kc[x] + 1
                q.append(y)
    kq.append(kc[T])
print("\n".join(map(str, kq)))
