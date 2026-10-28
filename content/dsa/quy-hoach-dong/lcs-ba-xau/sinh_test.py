"""Sinh input cho bài dãy con chung dài nhất của ba xâu: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
lim = {"nho": 6, "tb": 15}.get(co, 3 if seed <= 2 else 60)
chu = "abc" if seed % 2 else "abcdefgh"
for _ in range(3):
    print("".join(r.choice(chu) for _ in range(r.randint(1 if co != "lon" or seed <= 2 else 50, lim))))
