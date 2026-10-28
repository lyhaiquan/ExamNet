"""Sinh input cho bài tìm từ trong lưới chữ: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
m, n = (r.randint(1, 3), r.randint(1, 3)) if co == "nho" else (r.randint(2, 5), r.randint(2, 5))
chu = "ab" if seed % 2 else "abcde"
luoi = ["".join(r.choice(chu) for _ in range(n)) for _ in range(m)]
q = r.randint(1, 5)
print(m, n)
print(*luoi, sep="\n")
print(q)
for _ in range(q):
    if r.random() < 0.5:
        # đi ngẫu nhiên trên lưới để có từ chắc chắn tìm được
        i, j = r.randrange(m), r.randrange(n)
        da, tu = {(i, j)}, luoi[i][j]
        for _ in range(r.randint(0, 8)):
            ke = [(x, y) for x, y in ((i - 1, j), (i + 1, j), (i, j - 1), (i, j + 1))
                  if 0 <= x < m and 0 <= y < n and (x, y) not in da]
            if not ke:
                break
            i, j = r.choice(ke)
            da.add((i, j))
            tu += luoi[i][j]
        print(tu)
    else:
        print("".join(r.choice(chu) for _ in range(r.randint(1, 9))))
