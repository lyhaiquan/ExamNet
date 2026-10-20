"""Sinh input cho bài tìm kiếm tuần tự: python sinh_test.py <nho|tb|lon> <seed>.

Seed lẻ: x có trong dãy (test nhỏ còn có thể xuất hiện hai lần, phải in vị trí đầu); seed chẵn: không có.
Test lớn có x ở đầu, x ở cuối, một phần tử.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "nho":
    a = [r.randint(1, 20) for _ in range(r.randint(6, 9))]
elif co == "tb":
    a = [r.randint(1, 50) for _ in range(20)]
else:
    n = 1 if seed == 1 else 10**5
    a = [r.randint(-10**9, 10**9) for _ in range(n)]
if seed % 2:
    x = a[0] if seed == 3 else a[-1] if seed == 5 else r.choice(a)
    if co != "lon":
        a[r.randrange(len(a))] = x
else:
    x = r.randint(-10**9, -10**9 + 100) if co == "lon" else r.randint(60, 99)
print(len(a), x)
print(*a)
