from collections import deque

R, C = map(int, input().split())
luoi = [input() for _ in range(R)]
t = [[-1] * C for _ in range(R)]
q = deque()
