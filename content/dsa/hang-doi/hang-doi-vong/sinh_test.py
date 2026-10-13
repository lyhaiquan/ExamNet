"""Sinh input cho bài hàng đợi vòng: python sinh_test.py <nho|tb|lon> <seed>.

Tỉ lệ thêm/lấy đổi theo từng đoạn để hàng lúc đầy (có DAY), lúc rỗng (có RONG) và quay vòng nhiều lần.
Test lớn có biên: k = 1, lấy từ hàng rỗng liên tục, và k lớn với hàng đầy gần hết.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

CO_DINH = {  # hai test học viên xem được: có DAY, có quay vòng, có RONG
    1: (3, ["them 4", "them 7", "them 1", "them 9", "lay", "them 9", "lay", "lay", "lay", "lay"]),
    2: (4, ["them 3", "them 5", "lay", "lay", "them 8", "them 2", "them 6", "them 1", "them 7", "lay", "lay"]),
}
if co == "nho" and seed in CO_DINH:
    k, lenh = CO_DINH[seed]
    print(k, len(lenh))
    print(*lenh, sep="\n")
    sys.exit()
if co == "nho":
    k, q = r.randint(3, 4), r.randint(8, 12)
elif co == "tb":
    k, q = 4, 16
elif seed == 1:
    k, q = 1, 100_000
elif seed == 2:
    k, q = 10, 100_000
else:
    k, q = r.randint(1000, 100_000), 100_000

lenh, so, p_them = [], 0, 0.6
for t in range(q):
    if t % max(1, q // 8) == 0:
        p_them = r.choice([0.2, 0.5, 0.8])
    if co == "lon" and seed == 2:
        p_them = 0.3
    if r.random() < p_them:
        lenh.append(f"them {r.randint(1, 99 if co != 'lon' else 10**6)}")
        so = min(k, so + 1)
    else:
        lenh.append("lay")
        so = max(0, so - 1)
print(k, q)
print("\n".join(lenh))
