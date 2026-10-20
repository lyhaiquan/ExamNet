import viz

T = viz.Cay(ten="T")


def cao(nut):
    return 0 if nut is None else nut.cao


def cap_nhat(nut):
    nut.cao = 1 + max(cao(nut.trai), cao(nut.phai))


def quay_phai(y):
    """Con trái x lên thay y: y.trai = x.phai; x.phai = y; cập nhật cao y rồi x. Trả x."""
    return y


def quay_trai(x):
    """Đối xứng quay_phai. Trả gốc mới."""
    return x


def chen(nut, k):
    """Chèn k vào cây con gốc nut, cân bằng lại, trả gốc mới của cây con."""
    # nut is None: m = T.Nut(k); m.cao = 1; trả m
    # Chèn đệ quy sang trái / phải như cây nhị phân tìm kiếm (bằng thì trả nut luôn)
    # cap_nhat(nut); lech = cao(nut.trai) - cao(nut.phai)
    # lech > 1:  k < nut.trai.khoa → quay_phai(nut);  ngược lại nut.trai = quay_trai(nut.trai), quay_phai(nut)
    # lech < -1: k > nut.phai.khoa → quay_trai(nut);  ngược lại nut.phai = quay_phai(nut.phai), quay_trai(nut)
    return nut


def duyet_truoc(nut, kq):
    if nut is not None:
        kq.append(nut.khoa)
        duyet_truoc(nut.trai, kq)
        duyet_truoc(nut.phai, kq)


n = int(input())
for k in map(int, input().split()):
    T.goc = chen(T.goc, k)
kq = []
duyet_truoc(T.goc, kq)
print(*kq)
