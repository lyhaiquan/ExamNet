import sys

data = sys.stdin.read().split()
m, n = int(data[0]), int(data[1])
truoc = [0] * (n + 1)
lon_nhat = 0
for hang in data[2 : 2 + m]:
    nay = [0] * (n + 1)
    for j in range(1, n + 1):
        if hang[j - 1] == "1":
            nay[j] = 1 + min(truoc[j], nay[j - 1], truoc[j - 1])
    lon_nhat = max(lon_nhat, max(nay))
    truoc = nay
print(lon_nhat)
