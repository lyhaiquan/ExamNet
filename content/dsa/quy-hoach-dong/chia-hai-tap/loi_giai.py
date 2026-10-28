n = int(input())
a = list(map(int, input().split()))
T = sum(a)
bit = 1  # bit s bật nghĩa là lấy được tổng s
for x in a:
    bit |= bit << x
print(min(abs(T - 2 * s) for s in range(T + 1) if bit >> s & 1))
