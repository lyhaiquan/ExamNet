import sys

data = sys.stdin.read().split()
n, k = int(data[0]), int(data[1])
a = list(map(int, data[2 : 2 + n]))


def lam_duoc(M):
    doan, tong = 1, 0
    for x in a:
        if tong + x > M:
            doan += 1
            tong = x
        else:
            tong += x
    return doan <= k


lo, hi = max(a), sum(a)
while lo < hi:
    giua = (lo + hi) // 2
    if lam_duoc(giua):
        hi = giua
    else:
        lo = giua + 1
print(lo)
