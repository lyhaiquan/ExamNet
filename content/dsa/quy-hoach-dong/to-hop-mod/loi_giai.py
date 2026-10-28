import sys

MOD = 10**9 + 7
N = 1000
C = [[1]]
for n in range(1, N + 1):
    truoc = C[-1]
    C.append([1] + [(truoc[k - 1] + truoc[k]) % MOD for k in range(1, n)] + [1])
data = sys.stdin.read().split()
q = int(data[0])
print("\n".join(str(C[int(data[1 + 2 * i])][int(data[2 + 2 * i])]) for i in range(q)))
