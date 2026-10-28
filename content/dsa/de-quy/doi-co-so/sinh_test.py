"""Sinh input cho bài đổi sang hệ cơ số b: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
t = {"nho": r.randint(1, 5), "tb": r.randint(5, 30)}.get(co, 3 if seed <= 2 else 10_000)
print(t)
for _ in range(t):
    n = r.choice([0, 1, r.randint(0, 100), r.randint(0, 10**18), 10**18])
    print(n, r.randint(2, 36))
