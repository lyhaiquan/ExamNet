"""Sinh input cho bài hình chữ nhật lớn nhất trong biểu đồ cột: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 8), 8
elif co == "tb":
    n, lim = r.randint(10, 50), 30
else:
    n, lim = (r.randint(1, 3), 3) if seed <= 2 else (r.randint(50_000, 100_000), 10**9)
h = [r.randint(0, lim) for _ in range(n)]
if co == "lon" and seed in (3, 4):
    h.sort(reverse=seed == 4)
print(n)
print(*h)
