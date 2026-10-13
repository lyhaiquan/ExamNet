"""Sinh input cho bài BFS: python sinh_test.py <nho|tb|lon> <seed>.

Có đồ thị nhiều thành phần (đỉnh không tới được), đường dài (khoảng cách lớn), cạnh lặp.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n = r.randint(1, 7)
    m = r.randint(0, min(10, n * (n - 1) // 2))
elif co == "tb":
    n = r.randint(8, 10)
    m = r.randint(n - 1, 14)
else:
    n = r.randint(25_000, 50_000) if seed > 2 else r.randint(1, 4)
    m = min(100_000, r.randint(n - 1, 2 * n)) if seed > 2 else r.randint(0, 3)

canh = []
if co == "lon" and seed % 3 == 0 and n > 1:
    # một đường dài 1–2–…–k, phần còn lại ngẫu nhiên
    k = n // 2
    canh = [(i, i + 1) for i in range(1, k)]
while len(canh) < m and n > 1:
    u, v = r.randint(1, n), r.randint(1, n)
    if u != v:
        canh.append((u, v))
canh = canh[:m] if n > 1 else []
r.shuffle(canh)
print(n, len(canh))
for u, v in canh:
    print(u, v)
