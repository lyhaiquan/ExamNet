"""Sinh input cho bài đường đi ngắn nhất (đồ thị lớn): python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 6), "tb": r.randint(6, 30)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(100_000, 200_000))
m = {"nho": r.randint(1, 10), "tb": r.randint(10, 80)}.get(co, r.randint(1, 4) if seed <= 2 else 200_000)
lim = {"nho": 10, "tb": 50}.get(co, 10**9 if seed % 2 else 10)
print(n, m)
for i in range(m):
    if co == "lon" and seed > 2 and i < n - 1 and seed % 3 == 0:
        print(i + 1, i + 2, r.randint(0, lim))  # một đường dài qua mọi đỉnh
    else:
        print(r.randint(1, n), r.randint(1, n), r.randint(0, lim))
