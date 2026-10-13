"""Sinh input cho bài tập con tổng S: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Test `tb` (kiểm luật cắt nhánh) có ba số nhỏ và ba số lớn hơn S: duyệt hết thì
thu được gọi 127 lần, cắt nhánh thì chỉ vài chục. Test lớn có trường hợp biên: S lớn hơn tổng, S bằng tổng,
mọi số bằng nhau (nhiều cách nhất), số rất lớn.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")


def tong_tap_con(a):
    """S là tổng của một tập con ngẫu nhiên khác rỗng: chắc chắn có ít nhất một cách."""
    chon = [x for x in a if r.random() < 0.5] or [r.choice(a)]
    return sum(chon)


if co == "nho":
    a = [r.randint(1, 9) for _ in range(r.randint(4, 6))]
    S = tong_tap_con(a) if seed < 4 else sum(a) + 1
elif co == "tb":
    nho = [r.randint(1, 4) for _ in range(3)]
    S = nho[0] + nho[1]
    a = nho + [r.randint(S + 1, S + 20) for _ in range(3)]
    r.shuffle(a)
else:
    n = 18 if seed > 2 else r.randint(1, 18)
    if seed == 1:
        a = [r.randint(1, 100) for _ in range(n)]
        S = sum(a) + r.randint(1, 5)
    elif seed == 2:
        a = [r.randint(1, 100) for _ in range(n)]
        S = sum(a)
    elif seed == 3:
        a = [7] * n
        S = 7 * (n // 2)
    elif seed == 4:
        a = [r.randint(10**7, 10**8) for _ in range(n)]
        S = tong_tap_con(a)
    else:
        a = [r.randint(1, 60) for _ in range(n)]
        S = tong_tap_con(a) if seed % 2 else r.randint(1, sum(a))
print(len(a), S)
print(*a)
