import sys

data = sys.stdin.read().split()
n = int(data[0])
truoc2 = truoc1 = 0
for x in map(int, data[1 : n + 1]):
    truoc2, truoc1 = truoc1, max(truoc1, truoc2 + x)
print(truoc1)
