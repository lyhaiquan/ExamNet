"""Sinh input cho bài số nhị phân: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Test lớn có trường hợp biên ở seed đầu: n = 1, n là luỹ thừa của 2, n lớn nhất.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    n = r.randint(1, 8)
elif co == "tb":
    n = r.randint(20, 30)
elif seed == 1:
    n = 1
elif seed == 2:
    n = 1 << r.randint(10, 14)
elif seed == 3:
    n = 30_000
else:
    n = r.randint(1_000, 30_000)
print(n)
