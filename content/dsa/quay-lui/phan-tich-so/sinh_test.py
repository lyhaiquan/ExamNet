"""Sinh input cho bài phân tích thành tổng: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
print({"nho": r.randint(1, 6), "tb": r.randint(6, 12)}.get(co, seed if seed <= 2 else r.randint(20, 30)))
