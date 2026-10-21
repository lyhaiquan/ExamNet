"""Sinh input cho bài lửa lan trên lưới: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
R, C = {"nho": (r.randint(1, 4), r.randint(1, 4)), "tb": (r.randint(4, 12), r.randint(4, 12))}.get(
    co, (r.randint(1, 3), r.randint(1, 3)) if seed <= 2 else (r.randint(400, 500), r.randint(400, 500)))
p_da = [0.0, 0.3, 0.0][seed % 3] if co == "lon" else r.choice([0.0, 0.2])
so_f = r.randint(0, 3) if co != "lon" else r.randint(0 if seed <= 2 else 1, 5)
luoi = [["#" if r.random() < p_da else "." for _ in range(C)] for _ in range(R)]
if co == "lon" and seed % 3 == 2:
    # tường dọc có một lỗ hổng: lửa phải đi vòng, nhưng vẫn cháy hết
    for j in range(2, C, 4):
        lo = r.randrange(R)
        for i in range(R):
            luoi[i][j] = "." if i == lo else "#"
for _ in range(so_f):
    luoi[r.randrange(R)][r.randrange(C)] = "F"
print(R, C)
for hang in luoi:
    print("".join(hang))
