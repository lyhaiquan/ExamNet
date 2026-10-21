"""Sinh input cho bài sắp xếp theo tần suất: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 10), 4
elif co == "tb":
    n, lim = r.randint(10, 50), 10
else:
    n, lim = (r.randint(1, 3), 3) if seed <= 2 else (r.randint(50_000, 100_000), [10, 1000, 10**9][seed % 3])
print(n)
print(*[r.randint(-lim, lim) for _ in range(n)])
