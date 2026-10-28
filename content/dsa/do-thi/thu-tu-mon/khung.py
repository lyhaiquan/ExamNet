import heapq

n, m = map(int, input().split())
sau = [[] for _ in range(n + 1)]
thieu = [0] * (n + 1)
for _ in range(m):
    a, b = map(int, input().split())
    sau[a].append(b)
    thieu[b] += 1
san_sang = [v for v in range(1, n + 1) if thieu[v] == 0]
heapq.heapify(san_sang)
