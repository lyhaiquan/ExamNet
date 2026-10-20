"""Sinh input cho bài đống nhị phân: python sinh_test.py <nho|tb|lon> <seed>.

Tỉ lệ them/lay đổi theo đoạn: đống lớn dần rồi rút về rỗng (có RONG). Test lớn có số giảm dần (mỗi lần thêm vun
lên tới gốc), số bằng nhau.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    q = r.randint(8, 11)
elif co == "tb":
    q = 24
else:
    q = 10 if seed == 1 else 10**5
lenh, so = [], 0
for k in range(q):
    p_them = 0.75 if k < q * 0.6 else 0.3
    if co == "nho" and k < 4:
        p_them = 1
    if r.random() < p_them:
        x = (10**9 - k) if (co == "lon" and seed == 2) else 7 if (co == "lon" and seed == 3) else r.randint(1, 99)
        if co == "lon" and seed > 3:
            x = r.randint(-10**9, 10**9)
        lenh.append(f"them {x}")
        so += 1
    else:
        lenh.append("lay")
        so = max(0, so - 1)
print(q)
print(*lenh, sep="\n")
