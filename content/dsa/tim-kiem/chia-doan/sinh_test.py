"""Sinh input cho bài chia dãy thành k đoạn: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 7), 10
elif co == "tb":
    n, lim = r.randint(8, 25), 50
else:
    n, lim = (r.randint(1, 3), 5) if seed <= 2 else (r.randint(50_000, 100_000), 10**4)
k = r.randint(1, n) if co != "lon" or seed % 2 else r.randint(1, min(n, 50))
a = [r.randint(0, lim) for _ in range(n)]
print(n, k)
print(*a)
