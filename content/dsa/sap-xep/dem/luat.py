"""Luật soi trace bài sắp xếp đếm: phải đếm vào mảng dem, mỗi phần tử một lần tăng.

Sắp bằng so sánh (tự viết nổi bọt, chọn…) rồi ghi vào kq thì mảng dem không được ghi lần nào.
"""


def kiem(su_kien, dau_vao):
    n = len(dau_vao.split())
    ghi_dem = sum(1 for e in su_kien if e.get("t") == "set" and e.get("v") == "dem")
    if ghi_dem < n:
        return f"Hãy đếm vào mảng dem: {n} phần tử thì dem phải được tăng {n} lần, mới thấy {ghi_dem}"
    return None
