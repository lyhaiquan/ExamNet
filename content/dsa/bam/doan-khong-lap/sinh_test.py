"""Sinh input cho bài đoạn dài nhất không có số lặp: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 10), 4
elif co == "tb":
    n, lim = r.randint(10, 50), 15
else:
    n, lim = (r.randint(1, 3), 2) if seed <= 2 else (r.randint(50_000, 100_000), [50, 5000, 10**9][seed % 3])
print(n)
print(*[r.randint(-lim, lim) for _ in range(n)])
