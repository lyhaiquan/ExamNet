MOD = 10**9 + 7
m = int(input())
tu_dien = set(input().split())
s = input().strip()
dai = max(map(len, tu_dien))
f = [0] * (len(s) + 1)
f[0] = 1
for i in range(1, len(s) + 1):
    tong = 0
    for L in range(1, min(dai, i) + 1):
        if f[i - L] and s[i - L : i] in tu_dien:
            tong += f[i - L]
    f[i] = tong % MOD
print(f[len(s)])
