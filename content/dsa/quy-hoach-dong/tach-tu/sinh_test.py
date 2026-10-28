"""Sinh input cho bài tách xâu thành các từ: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
chu = "ab" if seed % 2 else "abcdef"
m = {"nho": r.randint(1, 4), "tb": r.randint(3, 10)}.get(co, r.randint(1, 2) if seed <= 2 else 1000)
if co == "lon" and seed > 2 and seed % 2:
    m = r.randint(4, 8)  # chữ cái "ab", từ ngắn: không lấy hết mọi từ, để mỗi xâu cho một đáp án khác
dai_toi_da = 3 if co != "lon" or seed % 2 else 20
tu = set()
for _ in range(50 * m):  # chữ cái ít thì không đủ m từ khác nhau: lấy bao nhiêu được bấy nhiêu
    if len(tu) == m:
        break
    tu.add("".join(r.choice(chu) for _ in range(r.randint(1, dai_toi_da))))
m = len(tu)
tu = sorted(tu)
r.shuffle(tu)
L = {"nho": r.randint(1, 10), "tb": r.randint(10, 40)}.get(co, r.randint(1, 5) if seed <= 2 else 5000)
s = ""
while len(s) < L:
    s += r.choice(tu) if r.random() < 0.95 else r.choice(chu)
print(m)
print(*tu)
print(s[:L])
