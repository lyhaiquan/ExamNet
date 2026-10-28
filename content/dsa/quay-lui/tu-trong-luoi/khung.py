m, n = map(int, input().split())
luoi = [input() for _ in range(m)]
q = int(input())
for _ in range(q):
    tu = input()
    # quay lui từ mỗi ô (i, j) có luoi[i][j] == tu[0]
    print("NO")
