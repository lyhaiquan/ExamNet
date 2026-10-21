def ucln(a, b):
    # UCLN(a, 0) = a; UCLN(a, b) = UCLN(b, a % b)
    return a


t = int(input())
for _ in range(t):
    a, b = map(int, input().split())
    print(ucln(a, b), a // ucln(a, b) * b)
