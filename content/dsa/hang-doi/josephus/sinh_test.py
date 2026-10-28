"""Sinh input cho bài bài toán josephus: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, k = r.randint(1, 8), r.randint(1, 8)
elif co == "tb":
    n, k = r.randint(8, 30), r.randint(1, 40)
else:
    n, k = (r.randint(1, 3), r.randint(1, 5)) if seed <= 2 else (r.randint(500, 1000), r.randint(1, 1000))
print(n, k)
