"""Sinh input cho bài sắp xếp tô-pô: python sinh_test.py <nho|tb|lon> <seed>.

Đồ thị không vòng: lấy một hoán vị ngẫu nhiên, mọi cạnh đi từ trước ra sau trong hoán vị. Seed chẵn (test lớn)
thêm một cạnh ngược tạo vòng. Test lớn có đường thẳng dài, đỉnh cô lập, đồ thị dày.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")


def khong_vong(n, m):
    thu_tu = list(range(1, n + 1))
    r.shuffle(thu_tu)
    vt = {v: i for i, v in enumerate(thu_tu)}
    canh = set()
    while len(canh) < m:
        u, v = r.sample(range(1, n + 1), 2)
        if vt[u] > vt[v]:
            u, v = v, u
        canh.add((u, v))
    return thu_tu, list(canh)


if co == "nho":
    n = r.randint(5, 7)
    thu_tu, canh = khong_vong(n, n + 1)
    if seed == 2:  # có vòng
        canh.append((thu_tu[-1], thu_tu[0]))
elif co == "tb":
    n = 10
    thu_tu, canh = khong_vong(n, 14)
elif seed == 1:
    n, thu_tu, canh = 1, [1], []
elif seed == 3:
    n = 2 * 10**4
    thu_tu = list(range(1, n + 1))
    r.shuffle(thu_tu)
    canh = [(thu_tu[i], thu_tu[i + 1]) for i in range(n - 1)]
else:
    n = r.randint(10**4, 2 * 10**4)
    thu_tu, canh = khong_vong(n, 5 * 10**4 - 1)
    if seed % 2 == 0:
        i, j = sorted(r.sample(range(n), 2))
        if (thu_tu[j], thu_tu[i]) not in canh:
            canh.append((thu_tu[j], thu_tu[i]))
r.shuffle(canh)
print(n, len(canh))
for u, v in canh:
    print(u, v)
