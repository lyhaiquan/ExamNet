"""Sinh input cho bài đếm cặp nghịch thế: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 8), 5
elif co == "tb":
    n, lim = r.randint(10, 40), 20
else:
    n, lim = (r.randint(1, 3), 3) if seed <= 2 else (r.randint(50_000, 100_000), 10**9)
a = [r.randint(-lim, lim) for _ in range(n)]
if co == "lon" and seed == 3:
    a.sort(reverse=True)
elif co == "lon" and seed == 4:
    a.sort()
elif co == "lon" and seed == 5:
    a = [r.randint(0, 2) for _ in range(n)]
print(n)
print(*a)
