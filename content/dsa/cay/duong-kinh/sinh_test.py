"""Sinh input cho bài đường kính của cây: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 50)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
canh = []
for v in range(2, n + 1):
    u = v - 1 if co == "lon" and seed == 3 else r.randint(max(1, v - 3) if seed % 2 else 1, v - 1)
    canh.append((u, v))
nhan = list(range(1, n + 1))
r.shuffle(nhan)
print(n)
for u, v in canh:
    print(nhan[u - 1], nhan[v - 1])
