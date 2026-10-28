"""Sinh input cho bài truy vấn khoảng cách (floyd): python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 5), "tb": r.randint(5, 20)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(120, 150))
m = {"nho": r.randint(0, 8), "tb": r.randint(5, 60)}.get(co, r.randint(0, 3) if seed <= 2 else 10_000)
q = {"nho": r.randint(1, 5), "tb": r.randint(5, 20)}.get(co, 3 if seed <= 2 else 100_000)
lim = {"nho": 10, "tb": 100}.get(co, 10**6)
print(n, m, q)
for _ in range(m):
    print(r.randint(1, n), r.randint(1, n), r.randint(0, lim))
for _ in range(q):
    print(r.randint(1, n), r.randint(1, n))
