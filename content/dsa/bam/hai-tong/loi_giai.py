import sys

data = sys.stdin.read().split()
n, x = int(data[0]), int(data[1])
da_gap = {}
dem = 0
for v in map(int, data[2 : 2 + n]):
    dem += da_gap.get(x - v, 0)
    da_gap[v] = da_gap.get(v, 0) + 1
print(dem)
