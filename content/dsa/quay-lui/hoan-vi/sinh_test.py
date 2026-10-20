"""Sinh input cho bài sinh hoán vị: python sinh_test.py <nho|tb|lon> <seed>.

Chỉ có một số n. Test lớn đi đủ n = 1 … 8 (n = 8 là 40 320 dòng).
"""

import sys

co, seed = sys.argv[1], int(sys.argv[2])
if co == "nho":
    n = [3, 2, 1, 4][(seed - 1) % 4]
elif co == "tb":
    n = 4
else:
    n = (seed - 1) % 8 + 1
print(n)
