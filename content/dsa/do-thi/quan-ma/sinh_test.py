"""Sinh input cho bài số bước ít nhất của quân mã: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 5), "tb": r.randint(4, 30)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(400, 500))
x1, y1, x2, y2 = (r.randint(1, n) for _ in range(4))
if co == "lon" and seed > 2 and seed % 2:
    x1, y1, x2, y2 = 1, 1, n, n
print(n)
print(x1, y1, x2, y2)
