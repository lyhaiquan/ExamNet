"""Sinh input cho bài đếm cách đặt n quân hậu: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
if co == "lon":
    print([1, 2, 12, 11, 10, 12, 9, 12, 3, 8][(seed - 1) % 10])
else:
    print(r.randint(1, 6) if co == "nho" else r.randint(6, 8))
