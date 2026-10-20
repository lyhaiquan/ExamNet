import viz

T = viz.Cay(ten="T")
n = int(input())
for k in map(int, input().split()):
    if T.goc is None:
        T.goc = T.Nut(k)
        continue
    p = T.goc
    while True:
        if k < p.khoa:
            if p.trai is None:
                p.trai = T.Nut(k)
                break
            p = p.trai
        else:
            if p.phai is None:
                p.phai = T.Nut(k)
                break
            p = p.phai


def tham(nut, kq):
    kq.append(nut.khoa)
    T.danh_dau(nut, "xong")


def truoc(nut, kq):
    if nut is not None:
        tham(nut, kq)
        truoc(nut.trai, kq)
        truoc(nut.phai, kq)


def giua(nut, kq):
    if nut is not None:
        giua(nut.trai, kq)
        tham(nut, kq)
        giua(nut.phai, kq)


def sau(nut, kq):
    if nut is not None:
        sau(nut.trai, kq)
        sau(nut.phai, kq)
        tham(nut, kq)


def theo_muc(kq):
    q = viz.HangDoi(ten="q")
    q.enq(T.goc)
    while q:
        u = q.deq()
        tham(u, kq)
        for con in (u.trai, u.phai):
            if con is not None:
                q.enq(con)


def xoa_mau(nut):
    if nut is not None:
        T.danh_dau(nut, None)
        xoa_mau(nut.trai)
        xoa_mau(nut.phai)


TEN = {
    truoc: "Duyệt trước: thăm nút rồi mới xuống cây con trái, cây con phải",
    giua: "Duyệt giữa: hết cây con trái mới thăm nút, ra các khoá tăng dần",
    sau: "Duyệt sau: thăm nút sau cùng, khi hai cây con đã xong",
}
for duyet in (truoc, giua, sau):
    viz.key(TEN[duyet])
    kq = []
    duyet(T.goc, kq)
    print(*kq)
    xoa_mau(T.goc)
viz.key("Theo mức: hàng đợi giữ các nút chờ thăm, con vào cuối hàng")
kq = []
theo_muc(kq)
print(*kq)
