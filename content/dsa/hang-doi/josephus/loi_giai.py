from collections import deque

n, k = map(int, input().split())
q = deque(range(1, n + 1))
bi_loai = []
while q:
    for _ in range((k - 1) % len(q)):
        q.append(q.popleft())
    bi_loai.append(q.popleft())
print(*bi_loai)
