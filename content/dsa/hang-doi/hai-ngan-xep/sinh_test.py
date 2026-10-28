"""Sinh input cho bài hàng đợi từ hai ngăn xếp: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
q = {"nho": r.randint(1, 10), "tb": r.randint(10, 60)}.get(co, r.randint(1, 4) if seed <= 2 else 200_000)
ds = []
for _ in range(q):
    t = r.choice([1, 1, 2, 3]) if seed % 2 else r.choice([1, 2, 3])
    ds.append(f"1 {r.randint(-10**9, 10**9)}" if t == 1 else str(t))
print(q)
print(*ds, sep="\n")
