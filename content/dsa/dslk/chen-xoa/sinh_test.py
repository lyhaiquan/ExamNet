"""Sinh input cho bài chèn, xoá trên danh sách liên kết: python sinh_test.py <nho|tb|lon> <seed>.

Theo dõi độ dài để mọi vị trí hợp lệ. Có chèn đầu, chèn cuối, xoá đầu, xoá cuối, xoá tới khi rỗng rồi chèn lại.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    n, q = r.randint(3, 5), r.randint(3, 5)
elif co == "tb":
    n, q = 8, 10
else:
    n, q = (0, 1000) if seed == 1 else (1000, 1000) if seed == 2 else (r.randint(1, 1000), 1000)
ban_dau = [r.randint(1, 99) for _ in range(n)]
dai, lenh = n, []
for k in range(q):
    xoa_het = co == "lon" and seed == 3 and k < q // 2  # nửa đầu xoá gần hết
    if dai > 0 and (xoa_het or r.random() < 0.45):
        lenh.append(f"xoa {r.choice([0, dai - 1, r.randrange(dai)])}")
        dai -= 1
    else:
        lenh.append(f"chen {r.choice([0, dai, r.randint(0, dai)])} {r.randint(1, 99)}")
        dai += 1
print(n, q)
print(*ban_dau)
print(*lenh, sep="\n")
