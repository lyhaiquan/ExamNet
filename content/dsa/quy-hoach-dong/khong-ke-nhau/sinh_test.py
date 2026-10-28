"""Sinh input cho bài tổng lớn nhất không lấy hai số kề nhau: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 15)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
lim = {"nho": 10, "tb": 100}.get(co, 10**9)
print(n)
print(*[r.randint(-lim, lim) for _ in range(n)])
