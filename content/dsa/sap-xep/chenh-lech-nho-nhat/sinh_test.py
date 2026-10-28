"""Sinh input cho bài chênh lệch nhỏ nhất: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(2, 8), 30
elif co == "tb":
    n, lim = r.randint(10, 40), 1000
else:
    n, lim = (2, 10**9) if seed <= 2 else (r.randint(50_000, 100_000), 10**9)
a = [r.randint(-lim, lim) for _ in range(n)]
if co == "lon" and seed == 1:
    a = [-(10**9), 10**9]
print(n)
print(*a)
