"""Sinh input cho bài tổ hợp có tổng s: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 4), "tb": r.randint(3, 7)}.get(co, r.randint(1, 2) if seed <= 2 else r.randint(6, 10))
S = {"nho": r.randint(1, 12), "tb": r.randint(10, 25)}.get(co, r.randint(1, 40))
print(n, S)
print(*r.sample(range(2, 41 if co == "lon" else 15), n))
