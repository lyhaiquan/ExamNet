"""Sinh input cho bài chia thành hai tập cân bằng: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 14)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(60, 100))
lim = {"nho": 20, "tb": 200}.get(co, 1000)
print(n)
print(*[r.randint(1, lim) for _ in range(n)])
