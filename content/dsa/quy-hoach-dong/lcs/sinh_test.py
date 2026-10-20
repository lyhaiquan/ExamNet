"""Sinh input cho bài LCS: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Test lớn có trường hợp biên ở seed đầu: một ký tự, hai xâu giống hệt nhau,
không có ký tự chung, một xâu là dãy con của xâu kia.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")


def xau(n, chu="ABCD"):
    return "".join(r.choice(chu) for _ in range(n))


if co == "nho":
    x, y = xau(r.randint(1, 7)), xau(r.randint(1, 7))
elif co == "tb":
    x, y = xau(r.randint(10, 14)), xau(r.randint(10, 14))
elif seed == 1:
    x, y = "A", xau(r.randint(1, 300))
elif seed == 2:
    x = xau(300)
    y = x
elif seed == 3:
    x, y = xau(300, "ABC"), xau(300, "XYZ")
elif seed == 4:
    y = xau(300)
    x = "".join(c for c in y if r.random() < 0.5) or "A"
else:
    x, y = xau(r.randint(150, 300), "ABCDEFGH"), xau(r.randint(150, 300), "ABCDEFGH")
print(x)
print(y)
