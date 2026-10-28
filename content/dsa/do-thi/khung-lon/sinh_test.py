"""Sinh input cho bài cây khung nhỏ nhất (đồ thị lớn): python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 6), "tb": r.randint(6, 30)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
m = {"nho": r.randint(0, 10), "tb": r.randint(5, 80)}.get(co, r.randint(0, 3) if seed <= 2 else 200_000)
lim = {"nho": 10, "tb": 100}.get(co, 10**9)
canh = []
if r.random() < 0.7 and n > 1:
    canh = [(r.randint(1, v - 1), v, r.randint(-lim, lim)) for v in range(2, n + 1)][:m]
while len(canh) < m:
    canh.append((r.randint(1, n), r.randint(1, n), r.randint(-lim, lim)))
r.shuffle(canh)
print(n, len(canh))
for c in canh:
    print(*c)
