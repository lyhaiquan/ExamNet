"""Luật soi trace bài băm địa chỉ mở: các lần ghi vào T phải đúng như dò tuyến tính trong đề.

Đề cố định cách đặt (ô đã xoá đầu tiên trên đường dò, không có thì ô trống nơi dừng) và cách xoá (ghi dấu XOA),
nên dãy lần ghi vào T là duy nhất. Dùng list hay đếm bằng mảng thì kết quả tim vẫn đúng nhưng T không có
lần ghi nào; xoá bằng cách làm ô trống trở lại cũng sai dãy này.
"""


def kiem(su_kien, dau_vao):
    dong = dau_vao.split("\n")
    m, q = map(int, dong[0].split())
    o = [None] * m
    can = []
    for d in dong[1 : q + 1]:
        lenh, x = d.split()
        x = int(x)
        vt, xoa_dau, cho = -1, -1, -1
        for k in range(m):
            i = (x % m + k) % m
            if o[i] is None:
                cho = xoa_dau if xoa_dau != -1 else i
                break
            if o[i] == "XOA":
                if xoa_dau == -1:
                    xoa_dau = i
            elif o[i] == x:
                vt = i
                break
        else:
            cho = xoa_dau
        if lenh == "them" and vt == -1:
            o[cho] = x
            can.append((cho, x))
        elif lenh == "xoa" and vt != -1:
            o[vt] = "XOA"
            can.append((vt, "XOA"))
    ghi = [(e["i"], e["x"]) for e in su_kien if e.get("t") == "set" and e.get("v") == "T"]
    if ghi != can:
        return ("Các lần ghi vào bảng T chưa đúng dò tuyến tính: số x đặt vào ô đã xoá đầu tiên trên đường dò từ "
                "ô x % m (không có thì ô trống nơi dừng), xoá thì ghi T[i] = viz.XOA")
    return None
