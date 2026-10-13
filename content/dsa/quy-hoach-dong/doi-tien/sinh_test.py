"""Sinh input cho bài đổi tiền: python sinh_test.py <nho|tb|lon> <seed>.

Test lớn có biên: không đổi được (mọi mệnh giá chẵn, S lẻ), chỉ có đồng 1, bộ mệnh giá làm tham lam sai, và
mệnh giá lớn hơn S.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho" and seed <= 2:
    S, xu = [(10, [1, 5, 6]), (11, [2, 5])][seed - 1]  # tham lam sai; phải lùi lại mới đổi được
elif co == "nho":
    S = r.randint(6, 12)
    xu = r.sample(range(1, 8), r.randint(2, 3))
elif co == "tb":
    S, xu = 20, [1, 5, 6, 9]
elif seed == 1:
    S = 2 * r.randint(2000, 4999) + 1
    xu = [2 * r.randint(1, 50) for _ in range(5)]
elif seed == 2:
    S, xu = 10**4, [1]
elif seed == 3:
    S, xu = r.randint(5000, 10**4), [1, 3, 4, 50, 51, 53]
elif seed == 4:
    S = r.randint(10, 100)
    xu = [r.randint(S + 1, 10**4) for _ in range(9)] + [r.randint(1, S)]
else:
    S = r.randint(5000, 10**4)
    xu = r.sample(range(2, 500), r.randint(3, 10))
print(len(xu), S)
print(*xu)
