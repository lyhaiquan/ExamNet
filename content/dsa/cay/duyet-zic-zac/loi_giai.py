import sys

data = sys.stdin.read().split()
n = int(data[0])
khoa, trai, phai = [0] * (n + 1), [0] * (n + 1), [0] * (n + 1)
for i in range(1, n + 1):
    khoa[i], trai[i], phai[i] = int(data[3 * i - 2]), int(data[3 * i - 1]), int(data[3 * i])
tang, so, dong = [1], 0, []
while tang:
    k = [khoa[v] for v in tang]
    if so % 2:
        k.reverse()
    dong.append(" ".join(map(str, k)))
    tang = [c for v in tang for c in (trai[v], phai[v]) if c]
    so += 1
print("\n".join(dong))
