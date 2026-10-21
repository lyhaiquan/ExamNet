A = input().strip()
B = input().strip()
C = input().strip()
la, lb, lc = len(A), len(B), len(C)
f = [[[0] * (lc + 1) for _ in range(lb + 1)] for _ in range(la + 1)]
for i in range(1, la + 1):
    for j in range(1, lb + 1):
        for k in range(1, lc + 1):
            if A[i - 1] == B[j - 1] == C[k - 1]:
                f[i][j][k] = f[i - 1][j - 1][k - 1] + 1
            else:
                f[i][j][k] = max(f[i - 1][j][k], f[i][j - 1][k], f[i][j][k - 1])
print(f[la][lb][lc])
