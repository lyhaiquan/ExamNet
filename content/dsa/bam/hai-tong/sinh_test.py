"""Sinh input cho bài đếm cặp có tổng bằng x: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 8), 5
elif co == "tb":
    n, lim = r.randint(10, 50), 10
else:
    n, lim = (r.randint(1, 3), 3) if seed <= 2 else (r.randint(50_000, 100_000), [3, 1000, 10**9][seed % 3])
a = [r.randint(-lim, lim) for _ in range(n)]
x = r.randint(-2 * lim, 2 * lim) if r.random() < 0.5 or n < 2 else a[0] + a[-1]
print(n, x)
print(*a)
