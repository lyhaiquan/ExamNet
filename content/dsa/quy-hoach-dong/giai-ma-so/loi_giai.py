MOD = 10**9 + 7
s = input().strip()
truoc2, truoc1 = 0, 1  # f[i - 2], f[i - 1]
for i, c in enumerate(s):
    nay = truoc1 if c != "0" else 0
    if i and s[i - 1] != "0" and 10 <= int(s[i - 1 : i + 1]) <= 26:
        nay += truoc2
    truoc2, truoc1 = truoc1, nay % MOD
print(truoc1)
