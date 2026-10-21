n = int(input())
khoa, trai, phai = [0] * (n + 1), [0] * (n + 1), [0] * (n + 1)
for i in range(1, n + 1):
    khoa[i], trai[i], phai[i] = map(int, input().split())
tang = [1]
