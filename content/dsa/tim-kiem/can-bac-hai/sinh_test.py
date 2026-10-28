"""Sinh input cho bài phần nguyên căn bậc hai: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
t = {"nho": r.randint(1, 5), "tb": r.randint(5, 30)}.get(co, 10_000 if seed > 2 else 5)
xs = []
for _ in range(t):
    kieu = r.randint(0, 3)
    if kieu == 0:
        xs.append(r.randint(0, 100))
    elif kieu == 1:
        s = r.randint(0, 10**9)
        xs.append(min(10**18, s * s + r.choice([-1, 0, 1]) if s else 0))
    else:
        xs.append(r.randint(0, 10**18))
if co == "lon" and seed == 1:
    xs = [0, 1, 10**18, 10**18 - 1, 999999999999999999]
xs = [max(0, x) for x in xs]
print(len(xs))
print(*xs, sep="\n")
