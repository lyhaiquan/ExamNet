import heapq
import sys

data = sys.stdin.buffer.read().split()
n, m = int(data[0]), int(data[1])
sau = [[] for _ in range(n + 1)]
thieu = [0] * (n + 1)
for i in range(m):
    a, b = int(data[2 + 2 * i]), int(data[3 + 2 * i])
    sau[a].append(b)
    thieu[b] += 1
san_sang = [v for v in range(1, n + 1) if thieu[v] == 0]
heapq.heapify(san_sang)
kq = []
while san_sang:
    u = heapq.heappop(san_sang)
    kq.append(u)
    for v in sau[u]:
        thieu[v] -= 1
        if thieu[v] == 0:
            heapq.heappush(san_sang, v)
print(*kq) if len(kq) == n else print(-1)
