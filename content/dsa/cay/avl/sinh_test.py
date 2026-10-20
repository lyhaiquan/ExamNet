"""Sinh input cho bài cây AVL: python sinh_test.py <nho|tb|lon> <seed>.

Test nhỏ dựng sẵn để có đủ bốn kiểu quay. Test lớn có dãy tăng, dãy giảm (cây thường thành đường thẳng, đệ quy
quá giới hạn; AVL thì không), khoá trùng, ngẫu nhiên.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
NHO = [
    [10, 20, 30, 40, 50, 25],  # phải – phải, rồi phải – trái
    [50, 40, 30, 20, 10, 35],  # trái – trái, rồi trái – phải
    [30, 10, 20],              # trái – phải
    [10, 30, 20, 30],          # phải – trái, khoá trùng
]
if co == "nho":
    a = NHO[(seed - 1) % 4]
elif co == "tb":
    a = r.sample(range(1, 100), 20)
elif seed == 1:
    a = list(range(1, 2 * 10**4 + 1))
elif seed == 2:
    a = list(range(2 * 10**4, 0, -1))
elif seed == 3:
    a = [r.randint(1, 50) for _ in range(2 * 10**4)]
else:
    a = [r.randint(-10**9, 10**9) for _ in range(2 * 10**4)]
print(len(a))
print(*a)
