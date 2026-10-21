import sys

data = sys.stdin.read().split()
n = int(data[0])
a = sorted(map(int, data[1 : n + 1]))
print(min(a[i + 1] - a[i] for i in range(n - 1)))
