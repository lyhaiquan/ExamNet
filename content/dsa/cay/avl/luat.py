"""Luật soi trace bài cây AVL: cây phải được dựng và quay trên chính cây T.

Dựng lại cây T từ trace (nút, cạnh) và so thứ tự duyệt trước với cây AVL đúng. Tính AVL bằng lớp tự viết rồi
chỉ in ra thì cây T không giống.
"""

def cay_cuoi(su_kien, ten="T"):
    """Dựng lại cây sau cùng từ trace (ảnh chụp đầu, node, set, edge); trả thứ tự duyệt trước các khoá."""
    goc, khoa, trai, phai = None, {}, {}, {}
    for e in su_kien:
        if e.get("t") == "snap" and isinstance(e.get("state"), dict) and ten in e["state"]:
            s = e["state"][ten]
            goc = s.get("goc")
            for nid, k, l, r, *_ in s.get("nut", []):
                khoa[nid], trai[nid], phai[nid] = k, l, r
            break
    for e in su_kien:
        if e.get("v") != ten:
            continue
        t = e.get("t")
        if t == "node":
            khoa[e["id"]], trai[e["id"]], phai[e["id"]] = e.get("x"), None, None
        elif t == "set":
            khoa[e["id"]] = e.get("x")
        elif t == "edge":
            if e.get("u") is None:
                goc = e.get("w")
            elif e.get("c") == "L":
                trai[e["u"]] = e.get("w")
            else:
                phai[e["u"]] = e.get("w")
    ra, ngan = [], [goc]
    while ngan and len(ra) <= len(khoa):
        p = ngan.pop()
        if p is None or p not in khoa:
            continue
        ra.append(khoa[p])
        ngan.append(phai.get(p))
        ngan.append(trai.get(p))
    return ra


class _Nut:
    def __init__(self, k):
        self.k, self.l, self.r, self.h = k, None, None, 1


def _h(n):
    return 0 if n is None else n.h


def _cn(n):
    n.h = 1 + max(_h(n.l), _h(n.r))


def _qp(y):
    x = y.l
    y.l, x.r = x.r, y
    _cn(y)
    _cn(x)
    return x


def _qt(x):
    y = x.r
    x.r, y.l = y.l, x
    _cn(x)
    _cn(y)
    return y


def _chen(n, k):
    if n is None:
        return _Nut(k)
    if k < n.k:
        n.l = _chen(n.l, k)
    elif k > n.k:
        n.r = _chen(n.r, k)
    else:
        return n
    _cn(n)
    lech = _h(n.l) - _h(n.r)
    if lech > 1:
        if k > n.l.k:
            n.l = _qt(n.l)
        return _qp(n)
    if lech < -1:
        if k < n.r.k:
            n.r = _qp(n.r)
        return _qt(n)
    return n


def _truoc(n, ra):
    if n is not None:
        ra.append(n.k)
        _truoc(n.l, ra)
        _truoc(n.r, ra)


def kiem(su_kien, dau_vao):
    so = dau_vao.split()
    goc = None
    for k in so[1 : 1 + int(so[0])]:
        goc = _chen(goc, int(k))
    dung = []
    _truoc(goc, dung)
    if cay_cuoi(su_kien) != dung:
        return "Cây T sau khi chèn hết chưa đúng cây AVL: hãy chèn và quay trên chính cây T (T.Nut, nut.trai, nut.phai)"
    return None
