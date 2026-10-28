import heapq
import sys

data = sys.stdin.read().split()
k = int(data[0])
day, i = [], 1
for _ in range(k):
    m = int(data[i])
    day.append(list(map(int, data[i + 1 : i + 1 + m])))
    i += 1 + m
dong = [(d[0], j, 0) for j, d in enumerate(day) if d]
heapq.heapify(dong)
kq = []
while dong:
    x, j, p = heapq.heappop(dong)
    kq.append(x)
    if p + 1 < len(day[j]):
        heapq.heappush(dong, (day[j][p + 1], j, p + 1))
print(*kq)
