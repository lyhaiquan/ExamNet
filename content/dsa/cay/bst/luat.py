"""Luật soi trace bài cây nhị phân tìm kiếm: các lệnh phải thực hiện trên chính cây T.

Dựng lại cây từ trace (nút, khoá, cạnh) và so thứ tự duyệt trước với cây đúng sau mọi lệnh (xoá nút hai con
bằng nút nhỏ nhất của cây con phải). Làm trên list hay dict rồi in thì cây T không giống.
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
        self.k, self.l, self.r = k, None, None


def _chen(n, k):
    if n is None:
        return _Nut(k)
    if k < n.k:
        n.l = _chen(n.l, k)
    elif k > n.k:
        n.r = _chen(n.r, k)
    return n


def _xoa(n, k):
    if n is None:
        return None
    if k < n.k:
        n.l = _xoa(n.l, k)
    elif k > n.k:
        n.r = _xoa(n.r, k)
    elif n.l is None:
        return n.r
    elif n.r is None:
        return n.l
    else:
        s = n.r
        while s.l is not None:
            s = s.l
        n.k = s.k
        n.r = _xoa(n.r, s.k)
    return n


def _truoc(n, ra):
    if n is not None:
        ra.append(n.k)
        _truoc(n.l, ra)
        _truoc(n.r, ra)


def kiem(su_kien, dau_vao):
    dong = dau_vao.split("\n")
    goc = None
    for d in dong[1 : 1 + int(dong[0])]:
        lenh, k = d.split()
        if lenh == "chen":
            goc = _chen(goc, int(k))
        elif lenh == "xoa":
            goc = _xoa(goc, int(k))
    dung = []
    _truoc(goc, dung)
    if cay_cuoi(su_kien) != dung:
        return ("Cây T sau mọi lệnh chưa đúng: hãy chèn, xoá trên chính cây T (T.Nut, nut.trai, nut.phai); "
                "xoá nút đủ hai con bằng nút nhỏ nhất của cây con phải")
    return None
