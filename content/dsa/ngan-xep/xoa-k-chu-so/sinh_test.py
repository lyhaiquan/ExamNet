"""Sinh input cho bài xoá k chữ số: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 14)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
chu = "0123456789" if seed % 2 else "0019"
s = r.choice("123456789") + "".join(r.choice(chu) for _ in range(n - 1))
if r.random() < 0.05:
    s = "0"
print(s)
print(r.randint(0, len(s)))
