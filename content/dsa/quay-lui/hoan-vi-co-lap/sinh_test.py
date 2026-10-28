"""Sinh input cho bài hoán vị của dãy có số trùng: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 4), "tb": r.randint(4, 6)}.get(co, seed if seed <= 2 else r.randint(6, 8))
lim = r.choice([1, 2, 100])
print(n)
print(*[r.randint(-lim, lim) for _ in range(n)])
