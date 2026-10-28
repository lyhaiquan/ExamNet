"""Sinh input cho bài đếm đường đi ngắn nhất: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
n = {"nho": r.randint(1, 7), "tb": r.randint(5, 9)}.get(co, r.randint(1, 3) if seed <= 2 else r.randint(50_000, 100_000))
m = {"nho": r.randint(0, 10), "tb": r.randint(5, 14)}.get(co, r.randint(0, 3) if seed <= 2 else 200_000)
canh = set()
if co == "lon" and seed > 2 and seed % 2:
    m = 4 * ((n - 1) // 3) + 50
    # chuỗi "kim cương": số đường đi ngắn nhất tăng theo cấp số nhân, cần lấy dư
    k = (n - 1) // 3
    for i in range(k):
        a, b, c, d = 3 * i + 1, 3 * i + 2, 3 * i + 3, 3 * i + 4
        canh |= {(a, b), (a, c), (b, d), (c, d)}
tran = n * (n - 1) // 2
while len(canh) < min(m, tran):
    u, v = r.randint(1, n), r.randint(1, n)
    if u != v and (u, v) not in canh and (v, u) not in canh:
        canh.add((u, v))
canh = list(canh)
r.shuffle(canh)
s, t = (1, 3 * ((n - 1) // 3) + 1) if co == "lon" and seed % 2 else (r.randint(1, n), r.randint(1, n))
print(n, len(canh), s, t)
for c in canh:
    print(*c)
