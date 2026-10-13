"""Sinh input cho bài băm địa chỉ mở: python sinh_test.py <nho|tb|lon> <seed>.

Số lệnh them không quá m / 2 (bảng luôn còn ô trống). Số chọn trong khoảng nhỏ hoặc cùng ô để có va chạm, dò
vượt qua ô đã xoá, và thêm lại vào ô đã xoá. Test lớn có cụm dài (mọi số cùng ô).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    m, q, lim = r.randint(7, 11), r.randint(8, 10), 40
elif co == "tb":
    m, q, lim = 11, 14, 50
elif seed == 1:
    m, q, lim = 2, 10, 10
elif seed == 2:
    m, q, lim = 4001, 4000, 10**9  # mọi số chia hết cho m: cùng ô 0, cụm dài
else:
    m, q, lim = 2 * 10**5, 10**5, 10**9 if seed % 2 else 3 * 10**5

them_con = m // 2
co_roi, lenh = [], []
for _ in range(q):
    k = r.random()
    if co_roi and k < 0.45:
        lenh.append(f"{r.choice(['tim', 'xoa', 'tim'])} {r.choice(co_roi)}")
        continue
    loai = r.choice(["them", "them", "tim", "xoa"])
    if loai == "them":
        if them_con == 0:
            loai = "tim"
        else:
            them_con -= 1
    x = m * r.randint(0, lim // m) if seed == 2 and co == "lon" else r.randint(0, lim)
    if loai == "them":
        co_roi.append(x)
    lenh.append(f"{loai} {x}")
print(m, q)
print(*lenh, sep="\n")
