import viz

T = viz.Cay(ten="T")
n = int(input())
for k in map(int, input().split()):  # dựng sẵn cây nhị phân tìm kiếm
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


def truoc(nut, kq):
    # nut khác None: kq.append(nut.khoa), truoc(nut.trai, kq), truoc(nut.phai, kq)
    pass


def giua(nut, kq):
    pass


def sau(nut, kq):
    pass


def theo_muc(kq):
    q = viz.HangDoi(ten="q")
    # q.enq(T.goc); khi q còn: u = q.deq(), kq.append(u.khoa), con trái rồi con phải (nếu có) vào q


for duyet in (truoc, giua, sau):
    kq = []
    duyet(T.goc, kq)
    print(*kq)
kq = []
theo_muc(kq)
print(*kq)
