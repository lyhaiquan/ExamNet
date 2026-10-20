"""Luật soi trace bài sắp xếp trộn: phải chia đôi đệ quy bằng hàm tron.

Chia tới đoạn một phần tử thì tron được gọi 2n − 1 lần (cây lời gọi tb có dưới 32 nút). Sắp bằng vòng lặp
(nổi bọt, chèn) thì tron được gọi một lần hoặc không lần nào.
"""


def kiem(su_kien, dau_vao):
    n = len(dau_vao.split())
    goi = sum(1 for e in su_kien if e.get("t") == "call" and e.get("v") == "tron")
    if n >= 3 and goi < n:
        return f"Hãy chia đôi đệ quy: tron(trai, phai) gọi lại tron cho hai nửa. Với {n} số mới thấy {goi} lời gọi tron"
    return None
