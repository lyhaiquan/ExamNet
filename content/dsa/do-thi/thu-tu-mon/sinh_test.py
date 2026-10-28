"""Sinh input cho bài thứ tự học các môn: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 7), "tb": r.randint(7, 30)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
m = {"nho": r.randint(0, 8), "tb": r.randint(5, 60)}.get(co, r.randint(0, 3) if seed <= 2 else 200_000)
thu_tu = list(range(1, n + 1))
r.shuffle(thu_tu)
vt = {v: i for i, v in enumerate(thu_tu)}
canh = []
if n > 1:
    for _ in range(m):
        a, b = r.sample(range(1, n + 1), 2)
        if vt[a] > vt[b]:
            a, b = b, a
        canh.append((a, b))
    if canh and r.random() < 0.3:
        a, b = canh[0]
        canh.append((b, a))
print(n, len(canh))
for c in canh:
    print(*c)
