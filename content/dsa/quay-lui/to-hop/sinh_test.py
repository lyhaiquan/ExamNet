"""Sinh input cho bài sinh tổ hợp: python sinh_test.py <nho|tb|lon> <seed>.

Test lớn có biên k = 1, k = n, và n = 18, k = 9 (48 620 dòng, nhiều nhất).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, k = [(4, 2), (5, 3), (3, 3), (5, 1)][(seed - 1) % 4]
elif co == "tb":
    n, k = 5, 3
elif seed == 1:
    n, k = 18, 1
elif seed == 2:
    n, k = 18, 18
elif seed == 3:
    n, k = 18, 9
else:
    n = r.randint(8, 18)
    k = r.randint(2, n - 1)
print(n, k)
