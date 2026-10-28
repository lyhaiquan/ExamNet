import heapq
import sys

data = sys.stdin.read().split()
n = int(data[0])
duoi, tren, kq = [], [], []
for x in map(int, data[1 : n + 1]):
    if not duoi or x <= -duoi[0]:
        heapq.heappush(duoi, -x)
    else:
        heapq.heappush(tren, x)
    if len(duoi) > len(tren) + 1:
        heapq.heappush(tren, -heapq.heappop(duoi))
    elif len(tren) > len(duoi):
        heapq.heappush(duoi, -heapq.heappop(tren))
    kq.append(-duoi[0])
print(*kq)
