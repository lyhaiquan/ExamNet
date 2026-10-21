import sys

data = sys.stdin.read().split()
n = int(data[0])
khoa, trai, phai = [0] * (n + 1), [0] * (n + 1), [0] * (n + 1)
for i in range(1, n + 1):
    khoa[i], trai[i], phai[i] = int(data[3 * i - 2]), int(data[3 * i - 1]), int(data[3 * i])
cap = [(trai[1], phai[1])]
dung = True
while cap and dung:
    x, y = cap.pop()
    if x == 0 and y == 0:
        continue
    if x == 0 or y == 0 or khoa[x] != khoa[y]:
        dung = False
        break
    cap.append((trai[x], phai[y]))
    cap.append((phai[x], trai[y]))
print("YES" if dung else "NO")
