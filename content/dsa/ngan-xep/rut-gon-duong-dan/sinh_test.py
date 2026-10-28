"""Sinh input cho bài rút gọn đường dẫn: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
t = {"nho": r.randint(1, 5), "tb": r.randint(5, 20)}.get(co, 3 if seed <= 2 else 1000)
ten = ["a", "b", "lan", "tai_lieu", "x1", "...", "..", ".", "", "..", "."]
ds = []
for _ in range(t):
    k = r.randint(0, {"nho": 6, "tb": 12}.get(co, 190))
    ds.append("/" + "/".join(r.choice(ten) for _ in range(k)))
print(t)
print(*ds, sep="\n")
