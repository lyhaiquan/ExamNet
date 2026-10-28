import sys

data = sys.stdin.buffer.read().split()
n, m = int(data[0]), int(data[1])
canh = sorted((int(data[4 + 3 * i]), int(data[2 + 3 * i]), int(data[3 + 3 * i])) for i in range(m))
cha = list(range(n + 1))


def tim(x):
    goc = x
    while cha[goc] != goc:
        goc = cha[goc]
    while cha[x] != goc:
        cha[x], x = goc, cha[x]
    return goc


tong, so_canh = 0, 0
for w, u, v in canh:
    a, b = tim(u), tim(v)
    if a != b:
        cha[a] = b
        tong += w
        so_canh += 1
print(tong if so_canh == n - 1 else "IMPOSSIBLE")
