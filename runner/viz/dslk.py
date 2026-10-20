"""Danh sách liên kết đơn "gắn camera" (spec §7.5) cho chèn, xoá, đảo ngược, trộn hai danh sách.

    ds = viz.DSLK.tu([1, 2, 3], ten="ds")     # hoặc ds = viz.DSLK(ten="ds"); ds.dau = ds.Nut(1) …
    p = ds.dau                                 # biến trỏ vào nút: tự hiện thành mũi tên "p" trên hình
    p.gia_tri                                  # → read: tô vàng nút đang xét
    p.tiep = ds.Nut(9)                         # → node, link (gán None là cắt)

Tên tiếng Anh dùng được như nhau: `val`/`data`, `next`, `ds.head`, `ds.Node(x)`. Các biến cục bộ đang trỏ vào
nút (`prev`, `cur`, `nxt`…) được bộ theo dõi từng dòng nhận ra và ghi `ptr`, học viên không phải gọi gì thêm.
"""

from . import recorder, theo_doi

MAU = ("dang-xet", "xong", "loai", "bo")


class DSLK:
    def __init__(self, ten="ds"):
        self.ten = ten
        self._dau = None
        self._nut = {}
        self._mau = {}
        self._tro = []  # [[tên biến, id nút]] đang hiện
        self._dem = 0
        recorder.current.register(ten, self)
        theo_doi.them_moc_dong(self._xem_tro)

    @classmethod
    def tu(cls, gia_tri, ten="ds"):
        """Danh sách có sẵn các giá trị theo thứ tự."""
        ds = cls(ten=ten)
        truoc = None
        for x in gia_tri:
            n = ds.Nut(x)
            if truoc is None:
                ds.dau = n
            else:
                truoc.tiep = n
            truoc = n
        return ds

    def Nut(self, x):  # noqa: N802 — đọc như tên lớp: ds.Nut(5)
        return NutDS(self, x)

    Node = Nut

    @property
    def dau(self):
        return self._dau

    @dau.setter
    def dau(self, nut):
        _kiem_nut(self, nut)
        if nut is not self._dau:
            self._dau = nut
            self._ghi({"t": "link", "id": None, "w": _id(nut)})

    head = dau

    def __iter__(self):
        """Duyệt giá trị từ đầu tới cuối (không ghi sự kiện), ví dụ để in: print(*ds)."""
        p, da_qua = self._dau, set()
        while p is not None and p.id not in da_qua:
            da_qua.add(p.id)
            yield p._x
            p = p._tiep

    def __len__(self):
        return sum(1 for _ in self)

    def danh_dau(self, nut, mau="xong"):
        if mau is not None and mau not in MAU:
            raise ValueError(f"mau phải là một trong {MAU}")
        _kiem_nut(self, nut)
        self._mau[nut.id] = mau
        self._ghi({"t": "mark", "id": nut.id, "m": mau})

    def _xem_tro(self, frame):
        tro = sorted(
            [ten, gt.id]
            for ten, gt in frame.f_locals.items()
            if isinstance(gt, NutDS) and gt._ds is self and not ten.startswith("_")
        )
        if tro != self._tro:
            self._tro = tro
            self._ghi({"t": "ptr", "p": tro})

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def trang_thai(self):
        return {
            "kieu": "dslk",
            "dau": _id(self._dau),
            "nut": [[n.id, n._x, _id(n._tiep), self._mau.get(n.id)] for n in self._nut.values()],
            "tro": [list(t) for t in self._tro],
        }

    def __repr__(self):
        return f"DSLK({list(self)!r})"


class NutDS:
    def __init__(self, ds, x):
        self._ds = ds
        self.id = ds._dem
        ds._dem += 1
        self._x = x
        self._tiep = None
        ds._nut[self.id] = self
        ds._ghi({"t": "node", "id": self.id, "x": x})

    @property
    def gia_tri(self):
        self._ds._ghi({"t": "read", "id": self.id})
        return self._x

    @gia_tri.setter
    def gia_tri(self, x):
        self._x = x
        self._ds._ghi({"t": "set", "id": self.id, "x": x})

    val = data = gia_tri

    @property
    def tiep(self):
        return self._tiep

    @tiep.setter
    def tiep(self, nut):
        _kiem_nut(self._ds, nut)
        if nut is not self._tiep:
            self._tiep = nut
            self._ds._ghi({"t": "link", "id": self.id, "w": _id(nut)})

    next = tiep

    def __repr__(self):
        return f"Nut({self._x!r})"


def _id(nut):
    return None if nut is None else nut.id


def _kiem_nut(ds, nut):
    if nut is not None and (not isinstance(nut, NutDS) or nut._ds is not ds):
        raise TypeError(f"chỉ nối được nút tạo bằng {ds.ten}.Nut(...) vào danh sách {ds.ten}")
