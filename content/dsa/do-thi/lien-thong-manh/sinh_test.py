"""Sinh input cho bài đếm thành phần liên thông mạnh: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 7), "tb": r.randint(7, 30)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
m = {"nho": r.randint(0, 10), "tb": r.randint(5, 60)}.get(co, r.randint(0, 3) if seed <= 2 else 200_000)
canh = []
if co == "lon" and seed > 2 and seed % 2:
    canh = [(i, i + 1) for i in range(1, n)] + [(n, 1)]  # một vòng dài
while len(canh) < m:
    u = r.randint(1, n)
    canh.append((u, r.randint(max(1, u - 5), min(n, u + 5)) if r.random() < 0.8 else r.randint(1, n)))
canh = canh[:m] if co != "lon" or seed % 2 == 0 else canh
print(n, len(canh))
for c in canh:
    print(*c)
