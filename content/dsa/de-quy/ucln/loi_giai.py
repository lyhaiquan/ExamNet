import sys


def ucln(a, b):
    return a if b == 0 else ucln(b, a % b)


data = sys.stdin.read().split()
t = int(data[0])
kq = []
for i in range(t):
    a, b = int(data[1 + 2 * i]), int(data[2 + 2 * i])
    g = ucln(a, b)
    kq.append(f"{g} {a // g * b}")
print("\n".join(kq))
