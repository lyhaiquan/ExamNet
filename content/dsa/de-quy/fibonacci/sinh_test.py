"""Sinh input cho bài Fibonacci: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    print(r.randint(0, 8))
elif co == "tb":
    print(r.randint(10, 15))
else:
    print([0, 1, 2, 90][seed - 1] if seed <= 4 else r.randint(40, 90))
