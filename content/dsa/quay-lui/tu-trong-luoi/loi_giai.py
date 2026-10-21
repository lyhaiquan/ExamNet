import sys

data = sys.stdin.read().split()
m, n = int(data[0]), int(data[1])
luoi = data[2 : 2 + m]
q = int(data[2 + m])
dung = [[False] * n for _ in range(m)]


def thu(i, j, tu, k):
    if luoi[i][j] != tu[k]:
        return False
    if k == len(tu) - 1:
        return True
    dung[i][j] = True
    for x, y in ((i - 1, j), (i + 1, j), (i, j - 1), (i, j + 1)):
        if 0 <= x < m and 0 <= y < n and not dung[x][y] and thu(x, y, tu, k + 1):
            dung[i][j] = False
            return True
    dung[i][j] = False
    return False


for tu in data[3 + m : 3 + m + q]:
    co = any(thu(i, j, tu, 0) for i in range(m) for j in range(n))
    print("YES" if co else "NO")
