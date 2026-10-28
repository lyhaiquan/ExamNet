"""Sinh input cho bài ucln và bcnn: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
t = {"nho": r.randint(1, 5), "tb": r.randint(5, 30)}.get(co, 3 if seed <= 2 else 100_000)
lim = {"nho": 30, "tb": 1000}.get(co, 10**9)
print(t)
for _ in range(t):
    g = r.randint(1, 1000) if r.random() < 0.5 else 1
    print(min(lim, g * r.randint(1, max(1, lim // g))), min(lim, g * r.randint(1, max(1, lim // g))))
