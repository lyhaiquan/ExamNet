"""Sinh input cho bài cây nhị phân tìm kiếm: python sinh_test.py <nho|tb|lon> <seed>.

Chèn khoá xáo ngẫu nhiên (cây cao cỡ 3·ln n, đệ quy không chạm giới hạn), xen lệnh tìm và xoá: xoá lá, nút
một con, nút hai con, khoá không có; xoá hết rồi chèn lại.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    q, lim = r.randint(8, 11), 99
elif co == "tb":
    q, lim = 20, 99
else:
    q, lim = (2 * 10**4, 10**9) if seed > 1 else (6, 5)
co_roi, lenh = [], []
for k in range(q):
    p = r.random()
    dau = co == "nho" and k < 6
    if co_roi and not dau and p < 0.3:
        x = r.choice(co_roi)
        lenh.append(f"xoa {x}")
        co_roi.remove(x)
    elif co_roi and not dau and p < 0.55:
        lenh.append(f"tim {r.choice(co_roi) if r.random() < 0.6 else r.randint(-lim, lim)}")
    else:
        x = r.randint(-lim, lim)
        lenh.append(f"chen {x}")
        if x not in co_roi:
            co_roi.append(x)
print(q)
print(*lenh, sep="\n")
