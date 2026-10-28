import sys

data = sys.stdin.read().split()
n = int(data[0])
a = list(map(int, data[1 : n + 1]))
dem = {}
for x in a:
    dem[x] = dem.get(x, 0) + 1
thu_tu = sorted(dem, key=lambda x: (-dem[x], x))
kq = []
for x in thu_tu:
    kq.extend([x] * dem[x])
print(*kq)
