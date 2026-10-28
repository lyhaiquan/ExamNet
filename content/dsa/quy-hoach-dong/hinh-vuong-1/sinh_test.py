"""Sinh input cho bài hình vuông toàn số 1 lớn nhất: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
m, n = {"nho": (r.randint(1, 5), r.randint(1, 5)), "tb": (r.randint(5, 15), r.randint(5, 15))}.get(
    co, (r.randint(1, 3), r.randint(1, 3)) if seed <= 2 else (r.randint(400, 500), r.randint(400, 500)))
p = [0.5, 0.8, 0.95][seed % 3]
print(m, n)
for _ in range(m):
    print("".join("1" if r.random() < p else "0" for _ in range(n)))
