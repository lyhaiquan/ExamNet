"""Sinh input cho bài nhân đôi và trừ một: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
t = {"nho": r.randint(1, 5), "tb": r.randint(5, 20)}.get(co, 3 if seed <= 2 else 100)
lim = {"nho": 20, "tb": 200}.get(co, 10**4)
print(t)
for _ in range(t):
    print(r.randint(1, lim), r.randint(1, lim))
