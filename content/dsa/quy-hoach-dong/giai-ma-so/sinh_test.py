"""Sinh input cho bài giải mã xâu số: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 30)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
chu = ["12", "1260", "0123456789", "1122"][seed % 4]
print("".join(r.choice(chu) for _ in range(n)))
