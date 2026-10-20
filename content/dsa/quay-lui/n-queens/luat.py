"""Luật soi trace bài n quân hậu: phải quay lui bằng hàm đệ quy dat.

Với n = 5, quay lui theo hàng gọi dat 54 lần. Viết sẵn các vòng lặp lồng nhau thì dat chỉ được gọi một lần.
"""


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "dat")
    if goi <= n:
        return f"Hãy quay lui: dat(ds) tự gọi lại chính nó cho hàng sau. Với n = {n} mới thấy {goi} lời gọi dat"
    return None
