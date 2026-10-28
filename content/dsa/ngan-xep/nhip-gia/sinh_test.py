"""Sinh input cho bài nhịp giá cổ phiếu: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 10), 10
elif co == "tb":
    n, lim = r.randint(10, 50), 50
else:
    n, lim = (r.randint(1, 3), 3) if seed <= 2 else (r.randint(50_000, 100_000), 10**9)
g = [r.randint(1, lim) for _ in range(n)]
if co == "lon" and seed == 3:
    g.sort()
print(n)
print(*g)
