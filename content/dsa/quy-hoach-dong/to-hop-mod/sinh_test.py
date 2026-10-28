"""Sinh input cho bài tổ hợp c(n, k) modulo: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
q = {"nho": r.randint(1, 5), "tb": r.randint(5, 30)}.get(co, 3 if seed <= 2 else 100_000)
lim = {"nho": 10, "tb": 100}.get(co, 1000)
print(q)
for _ in range(q):
    n = r.randint(0, lim)
    print(n, r.randint(0, n))
