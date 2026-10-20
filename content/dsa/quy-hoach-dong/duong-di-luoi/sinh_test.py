"""Sinh input cho bài đường đi tổng nhỏ nhất: python sinh_test.py <nho|tb|lon> <seed>.

Test lớn có biên: một ô, một hàng, một cột, lưới toàn 0, lưới 100 × 100.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    m, n = r.randint(3, 4), r.randint(3, 5)
elif co == "tb":
    m, n = 5, 6
else:
    m, n = {1: (1, 1), 2: (1, 100), 3: (100, 1), 4: (100, 100)}.get(seed, (r.randint(50, 100), r.randint(50, 100)))
print(m, n)
for _ in range(m):
    print(*(0 if co == "lon" and seed == 5 else r.randint(0, 9) for _ in range(n)))
