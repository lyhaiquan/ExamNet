"""Sinh input cho bài tổ tiên chung gần nhất: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 8), "tb": r.randint(8, 50)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
q = {"nho": r.randint(1, 5), "tb": r.randint(5, 30)}.get(co, 3 if seed <= 2 else 100_000)
cha = []
for i in range(2, n + 1):
    if co == "lon" and seed % 3 == 0:
        cha.append(i - 1)  # đường dài: leo từng bước sẽ chậm
    elif co == "lon" and seed % 3 == 1:
        cha.append(r.randint(max(1, i - 3), i - 1))
    else:
        cha.append(r.randint(1, i - 1))
print(n, q)
print(*cha)
for _ in range(q):
    print(r.randint(1, n), r.randint(1, n))
