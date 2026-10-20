"""Sinh input cho bài trò chơi trên bảng đen: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Input xem được (nho, tb) chỉ một test (t = 1): animation vẽ một đồ thị. Test
lớn trộn nhiều n nhỏ (đáp án "second" chỉ gặp khi n ≤ 83) với vài n tới 2000, tổng n ≤ 5000.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    ns = [{1: 12, 2: 9, 3: 16}.get(seed, r.randint(2, 18))]
elif co == "tb":
    ns = [r.randint(20, 30)]
elif seed == 1:
    ns = list(range(2, 42))  # mọi n nhỏ, đủ cả "first" lẫn "second"
elif seed == 2:
    ns = [2000, 1999, 83, 82]
else:
    ns = [r.randint(1000, 2000) for _ in range(r.randint(0, 2))]
    for _ in range(r.randint(10, 40 - len(ns))):
        n = r.randint(2, 100)
        if sum(ns) + n > 5000:
            break
        ns.append(n)
    r.shuffle(ns)
print(len(ns))
for n in ns:
    print(n)
