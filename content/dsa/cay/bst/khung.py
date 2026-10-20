import viz

T = viz.Cay(ten="T")


def chen(nut, k):
    """Chèn k vào cây con gốc nut, trả gốc (mới) của cây con."""
    # nut is None: trả T.Nut(k)
    # k < nut.khoa: nut.trai = chen(nut.trai, k);  k > nut.khoa: nut.phai = chen(nut.phai, k)
    return nut


def tim(nut, k):
    # Đi từ nut xuống: bằng thì True, nhỏ hơn sang trái, lớn hơn sang phải; gặp None thì False
    return False


def xoa(nut, k):
    """Xoá k khỏi cây con gốc nut, trả gốc (mới) của cây con."""
    # nut is None: trả None
    # k < nut.khoa: nut.trai = xoa(nut.trai, k);  k > nut.khoa: nut.phai = xoa(nut.phai, k)
    # k == nut.khoa:
    #   nut.trai is None: trả nut.phai;  nut.phai is None: trả nut.trai
    #   đủ hai con: s = nhỏ nhất của cây con phải; nut.khoa = s.khoa; nut.phai = xoa(nut.phai, s.khoa)
    return nut


def duyet_truoc(nut, kq):
    if nut is not None:
        kq.append(nut.khoa)
        duyet_truoc(nut.trai, kq)
        duyet_truoc(nut.phai, kq)


for _ in range(int(input())):
    lenh, k = input().split()
    k = int(k)
    if lenh == "chen":
        T.goc = chen(T.goc, k)
    elif lenh == "xoa":
        T.goc = xoa(T.goc, k)
    else:
        print("YES" if tim(T.goc, k) else "NO")
kq = []
duyet_truoc(T.goc, kq)
print(*kq) if kq else print("RONG")
