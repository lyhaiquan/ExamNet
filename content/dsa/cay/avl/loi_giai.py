import viz

T = viz.Cay(ten="T")


def cao(nut):
    return 0 if nut is None else nut.cao


def cap_nhat(nut):
    nut.cao = 1 + max(cao(nut.trai), cao(nut.phai))


def quay_phai(y):
    x = y.trai
    y.trai = x.phai
    x.phai = y
    cap_nhat(y)
    cap_nhat(x)
    return x


def quay_trai(x):
    y = x.phai
    x.phai = y.trai
    y.trai = x
    cap_nhat(x)
    cap_nhat(y)
    return y


def chen(nut, k):
    if nut is None:
        m = T.Nut(k)
        m.cao = 1
        return m
    x = nut.khoa
    if k < x:
        nut.trai = chen(nut.trai, k)
    elif k > x:
        nut.phai = chen(nut.phai, k)
    else:
        return nut
    cap_nhat(nut)
    lech = cao(nut.trai) - cao(nut.phai)
    if lech > 1:
        if k < nut.trai.khoa:
            viz.say(f"Nút {x} lệch trái {lech} (trái – trái): quay phải tại {x}")
            return quay_phai(nut)
        viz.say(f"Nút {x} lệch trái {lech} (trái – phải): quay trái tại con trái, rồi quay phải tại {x}")
        nut.trai = quay_trai(nut.trai)
        return quay_phai(nut)
    if lech < -1:
        if k > nut.phai.khoa:
            viz.say(f"Nút {x} lệch phải {-lech} (phải – phải): quay trái tại {x}")
            return quay_trai(nut)
        viz.say(f"Nút {x} lệch phải {-lech} (phải – trái): quay phải tại con phải, rồi quay trái tại {x}")
        nut.phai = quay_phai(nut.phai)
        return quay_trai(nut)
    return nut


def duyet_truoc(nut, kq):
    if nut is not None:
        kq.append(nut.khoa)
        duyet_truoc(nut.trai, kq)
        duyet_truoc(nut.phai, kq)


n = int(input())
for k in map(int, input().split()):
    viz.key(f"Chèn {k} như cây nhị phân tìm kiếm, rồi đi ngược về gốc kiểm độ lệch")
    T.goc = chen(T.goc, k)
kq = []
viz.key("Mọi nút lệch không quá 1. Duyệt trước để in")
duyet_truoc(T.goc, kq)
print(*kq)
