MOD = 10**9 + 7
N = 1000
C = [[0] * (N + 1) for _ in range(N + 1)]
# Điền tam giác Pascal
q = int(input())
for _ in range(q):
    n, k = map(int, input().split())
    print(C[n][k])
