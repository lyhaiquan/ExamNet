"""Sinh input cho bài tìm kiếm nhị phân: python sinh_test.py <nho|tb|lon> <seed>.

Câu hỏi trộn số có trong dãy và số không có (nhỏ hơn số đầu, lớn hơn số cuối, nằm giữa hai số kề nhau).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n = r.randint(8, 12)
    a = sorted(r.sample(range(1, 100), n))
    hoi = [r.choice(a), r.choice([x for x in range(1, 100) if x not in a])]
else:
    n, q = (30, 4) if co == "tb" else (1, 10) if seed == 1 else (10**5, 2 * 10**4)
    a = sorted(r.sample(range(-10**9, 10**9, 3), n))  # cách nhau ít nhất 3: luôn có số ở giữa hai số kề
    hoi = []
    for _ in range(q):
        k = r.random()
        if k < 0.5:
            hoi.append(r.choice(a))
        elif k < 0.6:
            hoi.append(a[0] - 1)
        elif k < 0.7:
            hoi.append(a[-1] + 1)
        else:
            hoi.append(r.choice(a) + 1)
print(n, len(hoi))
print(*a)
print(*hoi)
