from collections import deque

n = int(input())
ke = [[] for _ in range(n + 1)]
for _ in range(n - 1):
    u, v = map(int, input().split())
    ke[u].append(v)
    ke[v].append(u)


def bfs(nguon):
    # trả về (đỉnh xa nhất, khoảng cách tới nó)
    return nguon, 0
