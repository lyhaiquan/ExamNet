"""Sinh input cho bài sắp xếp đếm: python sinh_test.py <nho|tb|lon> <seed>.

Test lớn có biên: một số, mọi số bằng 0, mọi số bằng nhau, giá trị tới 10⁵, nhiều số trùng.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    a = [r.randint(0, 9) for _ in range(r.randint(6, 9))]
elif co == "tb":
    a = [r.randint(0, 20) for _ in range(30)]
elif seed == 1:
    a = [r.randint(0, 10**5)]
elif seed == 2:
    a = [0] * 10**5
elif seed == 3:
    a = [r.randint(0, 10**5)] * 10**5
elif seed == 4:
    a = [r.randint(0, 10**5) for _ in range(10**5)]
else:
    a = [r.randint(0, 100) for _ in range(r.randint(50_000, 10**5))]
print(*a)
