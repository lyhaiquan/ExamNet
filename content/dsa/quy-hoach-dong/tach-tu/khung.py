MOD = 10**9 + 7
m = int(input())
tu_dien = set(input().split())
s = input()
f = [0] * (len(s) + 1)
f[0] = 1
print(f[len(s)])
