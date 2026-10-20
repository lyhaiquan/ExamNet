"""Luật soi trace bài sinh tổ hợp: phải quay lui bằng hàm đệ quy thu.

Mỗi tổ hợp là một lá của cây lời gọi, nên thu được gọi ít nhất C(n, k) lần. Sinh bằng vòng lặp lồng nhau hay
thuật toán tổ hợp kế tiếp thì thu được gọi một lần hoặc không lần nào.
"""

import math


def kiem(su_kien, dau_vao):
    n, k = map(int, dau_vao.split()[:2])
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "thu")
    if goi < math.comb(n, k):
        return f"Hãy sinh bằng quay lui: thu(bat_dau, ds) tự gọi lại chính nó. Với n = {n}, k = {k} mới thấy {goi} lời gọi thu"
    return None
