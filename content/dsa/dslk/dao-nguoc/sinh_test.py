"""Sinh input cho bài đảo ngược danh sách liên kết: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n = r.randint(1, 6)
elif co == "tb":
    n = r.randint(8, 12)
else:
    n = r.randint(50_000, 100_000) if seed > 2 else r.randint(1, 2)
print(n)
print(*(r.randint(-10**9, 10**9) if co == "lon" else r.randint(0, 99) for _ in range(n)))
