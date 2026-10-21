import sys

data = sys.stdin.buffer.read().split()
n, q = int(data[0]), int(data[1])
cha = [0, 0] + [int(x) for x in data[2 : n + 1]]
sau = [0] * (n + 1)
for i in range(2, n + 1):
    sau[i] = sau[cha[i]] + 1
LOG = max(1, max(sau).bit_length())
len_ = [cha[:]]
for j in range(1, LOG):
    t = len_[-1]
    len_.append([t[t[v]] for v in range(n + 1)])
kq = []
i = n + 1
for _ in range(q):
    u, v = int(data[i]), int(data[i + 1])
    i += 2
    if sau[u] < sau[v]:
        u, v = v, u
    d = sau[u] - sau[v]
    j = 0
    while d:
        if d & 1:
            u = len_[j][u]
        d >>= 1
        j += 1
    if u != v:
        for j in range(LOG - 1, -1, -1):
            if len_[j][u] != len_[j][v]:
                u, v = len_[j][u], len_[j][v]
        u = cha[u]
    kq.append(u)
print("\n".join(map(str, kq)))
