"""Sinh input cho bài Dijkstra O(n²): python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Có đỉnh không đi tới được (đáp án −1), cạnh độ dài 0, và đồ thị mà đường
ít cạnh nhất không phải đường ngắn nhất (BFS sai).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")


def ngau_nhien(n, m, w_max, lien_thong=True):
    canh = {}
    if lien_thong:
        for v in range(1, n):
            canh[(r.randrange(v), v)] = r.randint(0, w_max)
    while len(canh) < m:
        u, v = r.sample(range(n), 2)
        canh[(min(u, v), max(u, v))] = r.randint(0, w_max)
    return [(u, v, w) for (u, v), w in canh.items()]


if co == "nho":
    n = r.randint(5, 7)
    canh = ngau_nhien(n, n + 2, 9)
    if seed == 2:  # một đỉnh lẻ loi
        n += 1
elif co == "tb":
    n, canh = 10, ngau_nhien(10, 16, 20)
elif seed == 1:
    n, canh = 1, []
elif seed == 2:
    n = 1000
    canh = ngau_nhien(600, 3000, 10**4) + [(u + 600, v + 600, w) for u, v, w in ngau_nhien(400, 2000, 10**4)]
elif seed == 3:
    # đường thẳng cạnh ngắn, và một cạnh tắt dài: đường ít cạnh nhất không phải ngắn nhất
    n = 1000
    canh = [(v, v + 1, 1) for v in range(n - 1)] + [(0, n - 1, 10**4)]
else:
    n = r.randint(700, 1000)
    canh = ngau_nhien(n, 10**4, 10**4 if seed % 2 else 5)
r.shuffle(canh)
print(n, len(canh))
for u, v, w in canh:
    print(*((v, u, w) if r.random() < 0.5 else (u, v, w)))
