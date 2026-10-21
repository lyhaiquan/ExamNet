import sys

data = sys.stdin.read().split()
n = int(data[0])
a = data[1 : n + 1]
cuoi = {}
l = dai_nhat = 0
for r, v in enumerate(a):
    if cuoi.get(v, -1) >= l:
        l = cuoi[v] + 1
    cuoi[v] = r
    dai_nhat = max(dai_nhat, r - l + 1)
print(dai_nhat)
