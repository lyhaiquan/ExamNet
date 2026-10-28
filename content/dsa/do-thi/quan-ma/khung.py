from collections import deque

n = int(input())
x1, y1, x2, y2 = map(int, input().split())
NUOC = [(1, 2), (2, 1), (2, -1), (1, -2), (-1, -2), (-2, -1), (-2, 1), (-1, 2)]
kc = [[-1] * (n + 1) for _ in range(n + 1)]
