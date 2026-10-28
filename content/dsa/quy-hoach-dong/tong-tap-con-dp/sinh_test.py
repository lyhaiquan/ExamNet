"""Sinh input cho bài tập con có tổng s: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 6), "tb": r.randint(6, 20)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(60, 100))
lim = {"nho": 15, "tb": 300}.get(co, 20_000)
a = [r.randint(1, lim if seed % 2 else lim // 20 + 1) for _ in range(n)]
S = r.randint(1, min(20_000, sum(a) + 5))
print(n, S)
print(*a)
