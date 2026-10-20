"""Luật soi trace bài đống nhị phân: chỉ thêm vào cuối, lấy ra ở cuối, đặt ô cuối lên gốc, đổi chỗ cha – con.

Giữ mảng luôn sắp (chèn vào giữa, sắp lại cả mảng) hay tìm min bằng cách duyệt hết thì kết quả vẫn đúng nhưng
sinh lần chèn giữa (ins), ghi cả mảng (all), lấy ra giữa mảng hay đổi chỗ hai ô không phải cha – con.
"""


def kiem(su_kien, dau_vao):
    sai = "Chỉ được thêm vào cuối (append), lấy ra ở cuối (pop), đặt ô cuối lên ô 0 và đổi chỗ nút cha với nút con"
    h, sau_pop, cho = [], False, None
    for e in su_kien:
        if e.get("v") != "h":
            continue
        t = e.get("t")
        if t == "snap":
            continue
        if t in ("ins", "all"):
            return sai
        if t == "push":
            if cho is not None:
                return sai
            h.append(e.get("x"))
            sau_pop = False
        elif t == "pop":
            if cho is not None or e.get("i") != len(h) - 1:
                return sai
            h.pop()
            sau_pop = True
        elif t == "set":
            i, x = e["i"], e.get("x")
            if cho is None and sau_pop and i == 0:
                h[0] = x  # ô cuối lên gốc
                sau_pop = False
            elif cho is None:
                cho = (i, x)
            else:
                (a, xa), (b, xb) = cho, (i, x)
                cha, con = min(a, b), max(a, b)
                if xa != h[b] or xb != h[a] or (con - 1) // 2 != cha:
                    return sai
                h[a], h[b] = xa, xb
                cho = None
    if cho is not None:
        return sai
    return None
