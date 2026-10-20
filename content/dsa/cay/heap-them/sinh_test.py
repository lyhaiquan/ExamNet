"""Sinh input cho bài thêm vào heap: python sinh_test.py <nho|tb|lon> <seed>.

Có test giảm dần (mỗi số mới đều nổi lên tới gốc), test nhiều số trùng nhau (dừng khi cha bằng con).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 8), 20
elif co == "tb":
    n, lim = r.randint(10, 15), 50
else:
    n, lim = (r.randint(50_000, 100_000), 10**9) if seed > 2 else (r.randint(1, 3), 5)
a = [r.randint(-lim, lim) for _ in range(n)]
if co == "lon" and seed % 3 == 0:
    a.sort(reverse=True)
elif co == "lon" and seed % 3 == 1:
    a = [r.randint(0, 3) for _ in range(n)]
print(n)
print(*a)
