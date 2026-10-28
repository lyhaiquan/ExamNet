import sys

CHU_SO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"


def doi(n, b):
    if n < b:
        return CHU_SO[n]
    return doi(n // b, b) + CHU_SO[n % b]


data = sys.stdin.read().split()
t = int(data[0])
print("\n".join(doi(int(data[1 + 2 * i]), int(data[2 + 2 * i])) for i in range(t)))
