"""Luật soi trace bài Kruskal: phải gộp mảnh bằng mảng cha (hợp và tìm).

Cây khung n đỉnh có n − 1 cạnh, mỗi cạnh lấy là một lần gộp, ghi vào cha ít nhất một lần. Prim, hay kiểm cùng
mảnh bằng cách duyệt đồ thị, thì mảng cha không được ghi lần nào.
"""


def kiem(su_kien, dau_vao):
    n = int(dau_vao.split()[0])
    ghi = sum(1 for e in su_kien if e.get("t") == "set" and e.get("v") == "cha")
    if ghi < n - 1:
        return (f"Hãy gộp mảnh bằng mảng cha: cây khung {n} đỉnh lấy {n - 1} cạnh, mỗi lần lấy gán cha[b] = a, "
                f"mới thấy {ghi} lần ghi cha")
    return None
