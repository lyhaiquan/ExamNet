n, S = map(int, input().split())
a = list(map(int, input().split()))
mat_na = (1 << (S + 1)) - 1
bit = 1
for x in a:
    bit = (bit | (bit << x)) & mat_na
print("YES" if bit >> S & 1 else "NO")
