import sys

data = sys.stdin.read().split()
n = int(data[0])
nhom = {}
for w in data[1 : n + 1]:
    nhom.setdefault("".join(sorted(w)), []).append(w)
print(len(nhom))
for ds in nhom.values():
    print(*ds)
