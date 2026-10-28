"""Sinh input cho bài gom các từ đảo chữ: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 40)}.get(co, r.randint(1, 3) if seed <= 2 else 50_000)
chu = "abc" if co != "lon" or seed % 2 else "abcdefghijklmnopqrstuvwxyz"
goc = ["".join(r.choice(chu) for _ in range(r.randint(1, 4 if co != "lon" else 20))) for _ in range(max(1, n // 3))]
tu = []
for _ in range(n):
    w = list(r.choice(goc))
    r.shuffle(w)
    tu.append("".join(w))
print(n)
print(*tu)
