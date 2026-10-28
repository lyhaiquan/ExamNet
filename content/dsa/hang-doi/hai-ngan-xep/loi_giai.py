import sys

data = sys.stdin.read().split()
q = int(data[0])
vao, ra, kq = [], [], []
i = 1
for _ in range(q):
    tt = data[i]
    if tt == "1":
        vao.append(data[i + 1])
        i += 2
        continue
    i += 1
    if not ra:
        while vao:
            ra.append(vao.pop())
    if not ra:
        kq.append("-1")
    elif tt == "2":
        kq.append(ra.pop())
    else:
        kq.append(ra[-1])
print("\n".join(kq))
