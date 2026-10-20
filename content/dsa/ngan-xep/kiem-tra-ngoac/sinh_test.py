"""Sinh input cho bài kiểm tra ngoặc: python sinh_test.py <nho|tb|lon> <seed>.

Seed cố định thì input cố định (server lưu đệm test ẩn theo hash nội dung bài). Một nửa số test là dãy hợp lệ
(sinh bằng cách lồng/nối ngoặc), nửa còn lại là dãy hợp lệ bị làm hỏng ở một chỗ: đổi loại một ngoặc, bỏ một
ngoặc, thêm ngoặc đóng thừa ở đầu, hoặc dư ngoặc mở ở cuối.
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")
MO, DONG = "([{", ")]}"


def hop_le(n_cap):
    """Dãy hợp lệ có n_cap cặp, sinh không đệ quy (n_cap tới 5·10⁴)."""
    out, st = [], []
    con = n_cap
    while con or st:
        if con and (not st or r.random() < 0.55):
            k = r.randrange(3)
            out.append(MO[k])
            st.append(k)
            con -= 1
        else:
            out.append(DONG[st.pop()])
    return out


if co == "nho":
    n_cap = r.randint(1, 5)
elif co == "tb":
    n_cap = r.randint(10, 15)
else:
    n_cap = r.randint(20_000, 49_999) if seed > 2 else r.randint(1, 3)

s = hop_le(n_cap)
if seed % 2 == 0:  # làm hỏng
    kieu = (seed // 2) % 4
    if kieu == 0:
        i = r.randrange(len(s))
        k = (MO + DONG).index(s[i])
        s[i] = (MO + DONG)[(k // 3) * 3 + (k % 3 + 1) % 3]  # cùng chiều, khác loại
    elif kieu == 1:
        del s[r.randrange(len(s))]
    elif kieu == 2:
        s.insert(0, DONG[r.randrange(3)])
    else:
        s.append(MO[r.randrange(3)])
print("".join(s) if s else "(")
