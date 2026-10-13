"""Luật soi trace bài sinh hoán vị: phải quay lui bằng hàm đệ quy thu.

Mỗi hoán vị là một lá của cây lời gọi, nên thu được gọi ít nhất n! lần. Sinh bằng vòng lặp (thuật toán hoán vị
kế tiếp) thì thu được gọi một lần hoặc không lần nào.
"""

import math


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "thu")
    if goi < math.factorial(n):
        return f"Hãy sinh bằng quay lui: thu(ds) tự gọi lại chính nó. Với n = {n} mới thấy {goi} lời gọi thu"
    return None
