"""Sinh input cho bài xâu con chung liên tiếp dài nhất: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 40)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(800, 1000))
m = {"nho": r.randint(1, 8), "tb": r.randint(8, 40)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(800, 1000))
chu = "ab" if seed % 2 else "abcdefghij"
A = "".join(r.choice(chu) for _ in range(n))
B = "".join(r.choice(chu) for _ in range(m))
print(A)
print(B)
