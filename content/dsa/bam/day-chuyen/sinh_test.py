"""Sinh input cho bài băm dây chuyền: python sinh_test.py <nho|tb|lon> <seed>.

Số chọn trong một khoảng nhỏ để lệnh tìm/xoá hay trúng số đã thêm; có test nhiều số cùng ô (dây chuyền dài).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    m, q, lim = r.randint(3, 7), r.randint(1, 10), 30
elif co == "tb":
    m, q, lim = r.randint(5, 7), r.randint(12, 16), 40
else:
    m, q = (r.randint(5_000, 10_000), r.randint(50_000, 100_000)) if seed > 2 else (r.randint(1, 3), r.randint(1, 5))
    if seed > 2 and seed % 3 == 0:
        q = 1_500  # mọi số cùng một ô: dây chuyền dài, tìm chậm dần (vẫn trong giới hạn thời gian)
    lim = 10**9 if seed % 2 else 3 * q

print(m, q)
co_roi = []
for _ in range(q):
    k = r.random()
    if co_roi and k < 0.45:
        lenh, x = r.choice(["tim", "xoa", "tim"]), r.choice(co_roi)
    else:
        lenh = r.choice(["them", "them", "tim", "xoa"])
        x = r.randint(0, lim) if not (co == "lon" and seed % 3 == 0) else m * r.randint(0, lim // m)  # cùng ô 0
        if lenh == "them":
            co_roi.append(x)
    print(lenh, x)
