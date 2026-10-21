"""Sinh input cho bài tìm trong dãy đã xoay vòng: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, q, lim = r.randint(1, 8), r.randint(1, 6), 20
elif co == "tb":
    n, q, lim = r.randint(10, 40), r.randint(5, 30), 100
else:
    n, q, lim = (1, 3, 10) if seed <= 2 else (r.randint(50_000, 100_000), 100_000, 10**9)
a = sorted(r.sample(range(-lim, lim + 1), n))
k = r.randint(0, n - 1)
a = a[k:] + a[:k]
xs = [r.choice(a) if r.random() < 0.6 else r.randint(-lim - 1, lim + 1) for _ in range(q)]
print(n, q)
print(*a)
print(*xs)
