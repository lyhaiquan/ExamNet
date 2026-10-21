"""Sinh input cho bài chu trình trong danh sách liên kết: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 40)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
p = list(range(2, n + 1))
r.shuffle(p)
dai = r.randint(1, n)
chuoi = [1] + p[: dai - 1]
tiep = [0] * (n + 1)
for a, b in zip(chuoi, chuoi[1:]):
    tiep[a] = b
if r.random() < 0.7:
    tiep[chuoi[-1]] = r.choice(chuoi)
for x in p[dai - 1 :]:
    tiep[x] = r.randint(0, n)
print(n)
print(*tiep[1:])
