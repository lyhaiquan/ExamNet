"""Sinh input cho bài liệt kê xâu nhị phân: python sinh_test.py <nho|tb|lon> <seed>."""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
print({"nho": r.randint(1, 5), "tb": r.randint(5, 12)}.get(co, seed if seed <= 2 else r.randint(15, 20)))
