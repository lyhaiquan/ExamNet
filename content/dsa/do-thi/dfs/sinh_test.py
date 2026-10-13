"""Sinh input cho bài DFS: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Số đỉnh bị xáo để cạnh không theo thứ tự đẹp. Test lớn có biên: một đỉnh,
không cạnh, đường thẳng 2·10⁴ đỉnh (đệ quy sâu quá giới hạn), đồ thị không liên thông, hình sao.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")


def ngau_nhien(n, m):
    canh = set()
    for v in range(2, n + 1):  # cây khung ngẫu nhiên: liên thông
        canh.add((r.randint(1, v - 1), v))
    while len(canh) < m:
        u, v = r.sample(range(1, n + 1), 2)
        canh.add((min(u, v), max(u, v)))
    return n, list(canh)


if co == "nho":
    n, canh = ngau_nhien(r.randint(6, 8), r.randint(8, 10))
elif co == "tb":
    n, canh = ngau_nhien(12, 16)
elif seed == 1:
    n, canh = 1, []
elif seed == 2:
    n, canh = 20_000, [(v, v + 1) for v in range(1, 20_000)]
elif seed == 3:
    n, canh = ngau_nhien(12_000, 30_000)
    n, canh = 20_000, canh + [(u + 12_000, v + 12_000) for u, v in ngau_nhien(8_000, 20_000)[1]]
elif seed == 4:
    n, canh = 20_000, [(1, v) for v in range(2, 20_001)]
else:
    n, canh = ngau_nhien(r.randint(12_000, 20_000), 50_000)

ten = list(range(1, n + 1))  # xáo số đỉnh, giữ đỉnh 1
r.shuffle(ten)
ten.remove(1)
ten = [1] + ten
canh = [(ten[u - 1], ten[v - 1]) for u, v in canh]
r.shuffle(canh)
canh = [(v, u) if r.random() < 0.5 else (u, v) for u, v in canh]
print(n, len(canh))
for u, v in canh:
    print(u, v)
