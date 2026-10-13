"""Sinh input cho bài tính hậu tố: python sinh_test.py <nho|tb|lon> <seed>.

Sinh thẳng biểu thức hậu tố bằng một ngăn xếp giá trị: mỗi bước hoặc đẩy một số, hoặc (khi ngăn xếp có từ hai
giá trị) ghép hai giá trị trên cùng bằng một phép. Phép `*` chỉ chọn khi tích không quá 10⁹, nên mọi giá trị
trung gian nhỏ hơn 10¹⁸. Test lớn có biểu thức một số, chuỗi phép trừ dài (kiểm thứ tự a − b), và biểu thức
dồn số trước rồi mới tính (ngăn xếp sâu).
"""

import random
import sys

co, seed = sys.argv[1], int(sys.argv[2])
r = random.Random(f"{co}-{seed}")


def hau_to(so_so, ghep=0.45, phep="+-*"):
    out, st, con = [], [], so_so
    while con or len(st) > 1:
        if con and (len(st) < 2 or r.random() > ghep):
            x = r.randint(0, 99)
            out.append(str(x))
            st.append(x)
            con -= 1
            continue
        b, a = st.pop(), st.pop()
        p = r.choice(phep)
        if p == "*" and abs(a * b) > 10**9:
            p = r.choice("+-")
        out.append(p)
        st.append(a + b if p == "+" else a - b if p == "-" else a * b)
    return " ".join(out)


if co == "nho":
    s = hau_to(r.randint(3, 5))
elif co == "tb":
    s = hau_to(10)
elif seed == 1:
    s = str(r.randint(0, 99))
elif seed == 2:
    s = hau_to(50_000, phep="-")
elif seed == 3:
    s = hau_to(50_000, ghep=0.0)
else:
    s = hau_to(r.randint(30_000, 50_000))
print(s)
