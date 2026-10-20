"""Sinh input cho bài nổi bọt: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định (server lưu đệm test ẩn theo hash nội dung bài). Test lớn có các trường hợp
biên ở seed đầu: dãy đã sắp, dãy ngược, mọi số bằng nhau, một phần tử, số âm và số rất lớn.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    a = [r.randint(-9, 20) for _ in range(r.randint(1, 8))]
elif co == "tb":
    # Đủ nhỏ để trace ghi đủ (≈ 1 500 sự kiện, dưới trần 10 000) khi kiểm luật đổi chỗ kề nhau
    a = [r.randint(-50, 99) for _ in range(r.randint(20, 30))]
else:
    n = r.randint(600, 1000)
    gioi_han = 10**9
    if seed == 1:
        a = sorted(r.randint(-gioi_han, gioi_han) for _ in range(n))
    elif seed == 2:
        a = sorted((r.randint(-gioi_han, gioi_han) for _ in range(n)), reverse=True)
    elif seed == 3:
        a = [r.randint(-gioi_han, gioi_han)] * n
    elif seed == 4:
        a = [r.randint(-gioi_han, gioi_han)]
    elif seed == 5:
        a = [r.randint(-3, 3) for _ in range(n)]  # nhiều số trùng
    else:
        a = [r.randint(-gioi_han, gioi_han) for _ in range(n)]

print(*a)
