"""Sinh input cho bài Floyd–Warshall: python sinh_test.py <nho|tb|lon> <seed>.

Có cạnh trùng (lấy cạnh ngắn hơn), cạnh độ dài 0, đỉnh không tới được, đồ thị đầy đủ.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, m, w = r.randint(4, 5), r.randint(5, 7), 9
elif co == "tb":
    n, m, w = 6, 12, 20
elif seed == 1:
    n, m, w = 1, 0, 1
elif seed == 2:
    n, m, w = 50, 2450, 10**4
else:
    n, m, w = r.randint(30, 50), r.randint(100, 2500), 10**4 if seed % 2 else 10
canh = []
if co == "lon" and seed == 2:
    canh = [(u, v, r.randint(0, w)) for u in range(n) for v in range(n) if u != v]
else:
    for _ in range(m):
        u, v = r.sample(range(n), 2)
        canh.append((u, v, r.randint(0, w)))
print(n, len(canh))
for c in canh:
    print(*c)
