"""Sinh input cho bài LIS O(n²): python sinh_test.py <nho|tb|lon> <seed>.

Test lớn có biên: một phần tử, dãy tăng hẳn (đáp án n), dãy giảm, mọi phần tử bằng nhau (tăng NGẶT nên đáp án
1), và số âm.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    a = [r.randint(1, 20) for _ in range(r.randint(5, 8))]
elif co == "tb":
    a = [r.randint(1, 50) for _ in range(12)]
elif seed == 1:
    a = [r.randint(-10**9, 10**9)]
elif seed == 2:
    a = sorted(r.sample(range(-10**9, 10**9), 1000))
elif seed == 3:
    a = sorted(r.sample(range(-10**9, 10**9), 1000), reverse=True)
elif seed == 4:
    a = [7] * 1000
else:
    a = [r.randint(-10**9, 10**9) if seed % 2 else r.randint(1, 100) for _ in range(r.randint(800, 1000))]
print(len(a))
print(*a)
