from collections import deque

t = int(input())
for _ in range(t):
    S, T = map(int, input().split())
    # BFS từ S: kc[x] = số bước ít nhất tới x
