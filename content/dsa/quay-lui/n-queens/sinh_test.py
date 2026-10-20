"""Sinh input cho bài n quân hậu: python sinh_test.py <nho|tb|lon> <seed>.

Chỉ có một số n. Test lớn đi n = 1 … 10 (n = 2, 3 không có cách nào).
"""

import sys

co, seed = sys.argv[1], int(sys.argv[2])
if co == "nho":
    n = [4, 3, 1, 5][(seed - 1) % 4]
elif co == "tb":
    n = 5
else:
    n = (seed - 1) % 10 + 1
print(n)
