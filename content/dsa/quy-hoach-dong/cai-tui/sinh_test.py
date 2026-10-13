"""Sinh input cho bài cái túi 0/1: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định. Test lớn có biên: mọi vật nặng hơn túi (đáp án 0), mọi vật vừa túi (lấy hết),
một vật, và bộ vật mà chọn tham lam theo giá hay theo giá/khối lượng đều sai.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")

if co == "nho":
    W = r.randint(5, 8)
    vat = [(r.randint(1, W), r.randint(1, 9)) for _ in range(r.randint(3, 4))]
elif co == "tb":
    W = 15
    vat = [(r.randint(1, 10), r.randint(1, 30)) for _ in range(6)]
elif seed == 1:
    W = r.randint(1, 50)
    vat = [(r.randint(W + 1, 1000), r.randint(1, 10**6)) for _ in range(100)]
elif seed == 2:
    vat = [(r.randint(1, 10), r.randint(1, 10**6)) for _ in range(100)]
    W = 1000
elif seed == 3:
    W = 1000
    vat = [(r.randint(1, 1000), r.randint(1, 10**6))]
elif seed == 4:
    # một vật giá cao, nặng gần đầy túi, và nhiều vật nhẹ cộng lại giá hơn
    W = 1000
    vat = [(990, 10**6)] + [(100, 120_000) for _ in range(10)] + [(r.randint(1, 1000), r.randint(1, 10**5)) for _ in range(89)]
    r.shuffle(vat)
else:
    W = r.randint(500, 1000)
    vat = [(r.randint(1, 300), r.randint(1, 10**6)) for _ in range(r.randint(60, 100))]
print(len(vat), W)
for w, v in vat:
    print(w, v)
