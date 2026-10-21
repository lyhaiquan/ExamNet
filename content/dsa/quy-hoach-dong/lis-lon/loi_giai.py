from bisect import bisect_left
import sys

data = sys.stdin.read().split()
n = int(data[0])
cuoi = []
for x in map(int, data[1 : n + 1]):
    k = bisect_left(cuoi, x)
    if k == len(cuoi):
        cuoi.append(x)
    else:
        cuoi[k] = x
print(len(cuoi))
