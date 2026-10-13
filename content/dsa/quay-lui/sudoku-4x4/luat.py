"""Luật soi trace bài Sudoku 4×4: phải quay lui bằng hàm đệ quy dien.

Mỗi ô trống được điền trong một lời gọi dien riêng, nên có bao nhiêu ô trống thì dien được gọi ít nhất bấy
nhiêu lần. Vòng lặp thử mọi cách điền (không đệ quy) thì dien được gọi một lần hoặc không lần nào.
"""


def kiem(su_kien, dau_vao):
    trong = dau_vao.count(".")
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "dien")
    if trong >= 2 and goi < trong:
        return f"Hãy quay lui: dien(i, j) gọi dien cho ô trống kế tiếp. Đề có {trong} ô trống mà mới thấy {goi} lời gọi dien"
    return None
