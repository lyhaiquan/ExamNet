"""Sinh input cho bài Fibonacci ghi nhớ: python sinh_test.py <nho|tb|lon> <seed>.

Chỉ có một số n. Test lớn có n = 0, 1, 45; đệ quy không ghi nhớ với n từ 30 trở lên thì quá giờ.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n = [6, 4, 1, 8][(seed - 1) % 4]
elif co == "tb":
    n = 12
else:
    n = {1: 0, 2: 1, 3: 45}.get(seed, r.randint(32, 45))
print(n)
