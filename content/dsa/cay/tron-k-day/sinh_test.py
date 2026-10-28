"""Sinh input cho bài trộn k dãy đã sắp: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
k = {"nho": r.randint(1, 4), "tb": r.randint(3, 10)}.get(co, r.randint(1, 3) if seed <= 2 else [2, 100, 10_000][seed % 3])
tong = {"nho": 12, "tb": 60}.get(co, 6 if seed <= 2 else 200_000)
lim = {"nho": 10, "tb": 100}.get(co, 10**9)
dai = [0] * k
for _ in range(r.randint(0, tong) if co != "lon" or seed <= 2 else tong):
    dai[r.randrange(k)] += 1
print(k)
for m in dai:
    print(m, *sorted(r.randint(-lim, lim) for _ in range(m)))
