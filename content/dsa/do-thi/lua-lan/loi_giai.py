from collections import deque
import sys

data = sys.stdin.read().split()
R, C = int(data[0]), int(data[1])
luoi = data[2 : 2 + R]
t = [-1] * (R * C)
q = deque()
co = 0
for i in range(R):
    for j, ch in enumerate(luoi[i]):
        if ch == "F":
            t[i * C + j] = 0
            q.append(i * C + j)
        elif ch == ".":
            co += 1
lau = 0
while q:
    x = q.popleft()
    i, j = divmod(x, C)
    for a, b in ((i - 1, j), (i + 1, j), (i, j - 1), (i, j + 1)):
        if 0 <= a < R and 0 <= b < C and luoi[a][b] == "." and t[a * C + b] < 0:
            t[a * C + b] = t[x] + 1
            lau = t[x] + 1
            co -= 1
            q.append(a * C + b)
print(lau if co == 0 else -1)
