import sys

data = sys.stdin.read().split()
n = int(data[0])
khoa, trai, phai = [0] * (n + 1), [0] * (n + 1), [0] * (n + 1)
for i in range(1, n + 1):
    khoa[i], trai[i], phai[i] = int(data[3 * i - 2]), int(data[3 * i - 1]), int(data[3 * i])
tang, thay = [1], []
while tang:
    thay.append(khoa[tang[0]])
    sau = []
    for v in tang:
        if trai[v]:
            sau.append(trai[v])
        if phai[v]:
            sau.append(phai[v])
    tang = sau
print(*thay)
