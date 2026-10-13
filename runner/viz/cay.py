"""Cây nhị phân "gắn camera" (spec §7.5) cho cây nhị phân tìm kiếm, AVL, các kiểu duyệt.

    T = viz.Cay(ten="T")
    T.goc = T.Nut(5)            # tạo nút → sự kiện node; gán gốc → edge (u = None)
    T.goc.trai = T.Nut(3)       # gán con → edge (c = "L"); gán None là cắt nhánh
    if k < nut.khoa: ...        # đọc khoá → read: bộ vẽ tô vàng nút đang xét

Tên tiếng Anh dùng được như nhau: `left`/`right`/`key`, `T.root`, `T.Node(5)`. Nút nhận thêm thuộc tính tuỳ ý
(ví dụ `nut.cao` cho AVL) như object thường; chỉ khoá và hai con được ghi lại. Xoay AVL chỉ là vài lần gán con,
bộ vẽ cho các nút trượt về chỗ mới nên thấy được phép xoay.
"""

from . import recorder

MAU = ("dang-xet", "xong", "loai", "bo")


class Cay:
    def __init__(self, ten="T"):
        self.ten = ten
        self._goc = None
        self._nut = {}  # id → nút, kể cả nút đã rời cây
        self._mau = {}
        self._da_gan = set()  # id các nút đã từng nằm trong cây: rời cây mà đứng một mình là đã bị xoá
        self._dem = 0
        recorder.current.register(ten, self)

    def Nut(self, khoa):  # noqa: N802 — đọc như tên lớp: T.Nut(5)
        return NutCay(self, khoa)

    Node = Nut

    @property
    def goc(self):
        return self._goc

    @goc.setter
    def goc(self, nut):
        _kiem_nut(self, nut)
        if nut is not self._goc:
            self._goc = nut
            if nut is not None:
                self._da_gan.add(nut.id)
            self._ghi({"t": "edge", "u": None, "c": None, "w": _id(nut)})

    root = goc

    def danh_dau(self, nut, mau="xong"):
        """Tô màu một nút và giữ màu: "dang-xet", "xong", "loai", "bo"; None để bỏ màu."""
        if mau is not None and mau not in MAU:
            raise ValueError(f"mau phải là một trong {MAU}")
        _kiem_nut(self, nut)
        self._mau[nut.id] = mau
        self._ghi({"t": "mark", "id": nut.id, "m": mau})

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def trang_thai(self):
        return {
            "kieu": "cay",
            "goc": _id(self._goc),
            "nut": [
                [n.id, n._khoa, _id(n._trai), _id(n._phai), self._mau.get(n.id), n.id in self._da_gan]
                for n in self._nut.values()
            ],
        }

    def __repr__(self):
        return f"Cay(ten={self.ten!r}, goc={self._goc!r})"


class NutCay:
    def __init__(self, cay, khoa):
        self._cay = cay
        self.id = cay._dem
        cay._dem += 1
        self._khoa = khoa
        self._trai = None
        self._phai = None
        cay._nut[self.id] = self
        cay._ghi({"t": "node", "id": self.id, "x": khoa})

    @property
    def khoa(self):
        self._cay._ghi({"t": "read", "id": self.id})
        return self._khoa

    @khoa.setter
    def khoa(self, k):
        self._khoa = k
        self._cay._ghi({"t": "set", "id": self.id, "x": k})

    key = khoa

    @property
    def trai(self):
        return self._trai

    @trai.setter
    def trai(self, nut):
        self._gan_con("L", nut)

    left = trai

    @property
    def phai(self):
        return self._phai

    @phai.setter
    def phai(self, nut):
        self._gan_con("R", nut)

    right = phai

    def _gan_con(self, ben, nut):
        _kiem_nut(self._cay, nut)
        cu = self._trai if ben == "L" else self._phai
        if nut is cu:
            return  # gán lại đúng con cũ (rất hay gặp trong chèn đệ quy): không có gì để vẽ
        if ben == "L":
            self._trai = nut
        else:
            self._phai = nut
        if nut is not None:
            self._cay._da_gan.add(nut.id)
        self._cay._ghi({"t": "edge", "u": self.id, "c": ben, "w": _id(nut)})

    def __repr__(self):
        return f"Nut({self._khoa!r})"


def _id(nut):
    return None if nut is None else nut.id


def _kiem_nut(cay, nut):
    if nut is not None and (not isinstance(nut, NutCay) or nut._cay is not cay):
        raise TypeError(f"chỉ gắn được nút tạo bằng {cay.ten}.Nut(...) vào cây {cay.ten}")
