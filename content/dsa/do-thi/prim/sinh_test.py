"""Sinh input cho bài Prim: python sinh_test.py <nho|tb|lon> <seed>.

Đồ thị liên thông (cây khung ngẫu nhiên rồi thêm cạnh). Test lớn có một đỉnh, đồ thị là cây, nhiều cạnh
cùng trọng số, đồ thị dày.
"""

import random
import sys


def lien_thong(n, m, w_max, r):
    """Đồ thị vô hướng liên thông n đỉnh 0..n-1, m cạnh không trùng, trọng số 1..w_max."""
    canh = {}
    for v in range(1, n):
        canh[(r.randrange(v), v)] = r.randint(1, w_max)
    while len(canh) < m:
        u, v = r.sample(range(n), 2)
        canh[(min(u, v), max(u, v))] = r.randint(1, w_max)
    ds = [(u, v, w) for (u, v), w in canh.items()]
    r.shuffle(ds)
    return [(v, u, w) if r.random() < 0.5 else (u, v, w) for u, v, w in ds]


co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n = r.randint(5, 7)
    canh = lien_thong(n, n + 2, 9, r)
elif co == "tb":
    n, canh = 10, lien_thong(10, 16, 20, r)
elif seed == 1:
    n, canh = 1, []
elif seed == 2:
    n, canh = 1000, lien_thong(1000, 999, 10**4, r)
elif seed == 3:
    n, canh = 1000, lien_thong(1000, 10**4, 3, r)
else:
    n = r.randint(700, 1000)
    canh = lien_thong(n, 10**4, 10**4, r)
print(n, len(canh))
for u, v, w in canh:
    print(u, v, w)
