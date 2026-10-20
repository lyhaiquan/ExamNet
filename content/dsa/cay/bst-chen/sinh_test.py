"""Sinh input cho bài chèn cây nhị phân tìm kiếm: python sinh_test.py <nho|tb|lon> <seed>.

Khoá đôi một khác nhau, xáo ngẫu nhiên (cây cao cỡ 3·ln n, chèn đệ quy không chạm giới hạn đệ quy).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(1, 8), 99
elif co == "tb":
    n, lim = r.randint(10, 15), 99
else:
    n, lim = (r.randint(10_000, 20_000), 10**9) if seed > 2 else (r.randint(1, 3), 10)
khoa = r.sample(range(-lim, lim + 1), n)
print(n)
print(*khoa)
