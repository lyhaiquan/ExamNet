"""Sinh input cho bài bốn kiểu duyệt cây: python sinh_test.py <nho|tb|lon> <seed>.

Khoá đôi một khác nhau, xáo ngẫu nhiên (cây cao cỡ 3·ln n, đệ quy không chạm giới hạn). Có cây một nút.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, lim = r.randint(5, 8), 99
elif co == "tb":
    n, lim = 12, 99
else:
    n, lim = (1, 10) if seed == 1 else (2 * 10**4, 10**9)
khoa = r.sample(range(-lim, lim + 1), n)
print(n)
print(*khoa)
