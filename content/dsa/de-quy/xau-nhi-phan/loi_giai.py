import sys

sys.setrecursionlimit(10000)
n = int(input())
kq = []


def dung(xau):
    if len(xau) == n:
        kq.append(xau)
        return
    dung(xau + "0")
    if not xau.endswith("1"):
        dung(xau + "1")


dung("")
print(len(kq))
print("\n".join(kq))
