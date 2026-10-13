"""Sinh input cho bài trung tố → hậu tố: python sinh_test.py <nho|tb|lon> <seed>.

Sinh từ trái sang phải: trước mỗi toán hạng có thể mở vài ngoặc, sau mỗi toán hạng có thể đóng vài ngoặc,
giữa hai toán hạng là một toán tử. Không đệ quy nên sinh được biểu thức 2·10⁵ ký tự. Test lớn có biểu thức
một chữ cái, chuỗi dài chỉ một mức ưu tiên (kiểm trái sang phải), và ngoặc lồng sâu.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
CHU = "abcdefghijklmnopqrstuvwxyz"


def bieu_thuc(so_hang, phep="+-*/", mo=0.25, dong=0.3, sau=40):
    out = []
    so_phep = []  # mỗi ngoặc đang mở: đã có bao nhiêu toán tử bên trong (chưa có thì chưa đóng, tránh "(a)")
    for k in range(so_hang):
        while len(so_phep) < sau and k < so_hang - 1 and r.random() < mo:
            out.append("(")
            so_phep.append(0)
        out.append(r.choice(CHU))
        while so_phep and so_phep[-1] and r.random() < dong:
            out.append(")")
            so_phep.pop()
        if k < so_hang - 1:
            out.append(r.choice(phep))
            if so_phep:
                so_phep[-1] += 1
    return "".join(out) + ")" * len(so_phep)


if co == "nho":
    s = bieu_thuc(r.randint(3, 5))
elif co == "tb":
    s = bieu_thuc(10)
elif seed == 1:
    s = r.choice(CHU)
elif seed == 2:
    s = bieu_thuc(50_000, phep="+-", mo=0, dong=0)
elif seed == 3:
    s = bieu_thuc(50_000, phep="*/", mo=0, dong=0)
elif seed == 4:
    s = bieu_thuc(30_000, mo=0.7, dong=0.05, sau=20_000)
else:
    s = bieu_thuc(r.randint(30_000, 60_000))
print(s)
