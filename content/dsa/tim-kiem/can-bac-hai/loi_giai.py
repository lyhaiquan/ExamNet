import sys


def can(n):
    lo, hi = 0, 10**9 + 1
    while lo < hi:
        giua = (lo + hi + 1) // 2
        if giua * giua <= n:
            lo = giua
        else:
            hi = giua - 1
    return lo


data = sys.stdin.read().split()
t = int(data[0])
print("\n".join(str(can(int(x))) for x in data[1 : 1 + t]))
