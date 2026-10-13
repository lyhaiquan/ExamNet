"""Đồ thị "gắn camera" (spec §7.5) cho BFS, DFS, Dijkstra, Prim, Kruskal, sắp xếp tô-pô.

    g = viz.DoThi(n, ten="g", bat_dau=1)        # đỉnh 1..n; co_huong=True cho đồ thị có hướng
    g.them_canh(u, v)                          # → edge; có trọng số: g.them_canh(u, v, w)
    for v in g.ke(u): ...                      # → read đỉnh u (tô vàng), rồi read từng cạnh u–v khi duyệt tới
    g.danh_dau(u, "xong")                      # tô màu đỉnh và giữ màu
    g.danh_dau_canh(u, v, "xong")              # ví dụ cạnh của cây BFS, cây khung nhỏ nhất
    g.nhan(v, d[v])                            # ghi chữ dưới đỉnh, ví dụ khoảng cách

Đồ thị có trọng số (`co_trong_so=True`) thì `g.ke(u)` cho từng cặp `(v, w)`. Đỉnh xếp quanh một vòng tròn theo
số thứ tự; người soạn bài muốn hình đẹp hơn thì đặt chỗ từng đỉnh bằng `g.vi_tri(u, x, y)` (x, y trong 0..1).
"""

from . import recorder

MAU = ("dang-xet", "xong", "loai", "bo")


class DoThi:
    def __init__(self, n, co_huong=False, co_trong_so=False, ten="g", bat_dau=0):
        self.ten = ten
        self.co_huong = co_huong
        self.co_trong_so = co_trong_so
        self._dinh = list(range(bat_dau, bat_dau + n))
        self._ke = {u: [] for u in self._dinh}
        self._canh = []  # [a, b, trọng số] theo thứ tự thêm
        self._co_canh = set()  # (u, v) có cạnh u → v: tô cạnh kiểm O(1), không dò danh sách kề (đỉnh bậc lớn)
        self._mau, self._mau_canh, self._nhan, self._vt = {}, {}, {}, {}
        recorder.current.register(ten, self)

    @property
    def so_dinh(self):
        return len(self._dinh)

    def dinh(self):
        """Danh sách đỉnh (không ghi sự kiện)."""
        return list(self._dinh)

    def __len__(self):
        return len(self._dinh)

    def them_canh(self, u, v, w=None):
        self._kiem(u)
        self._kiem(v)
        if self.co_trong_so and w is None:
            raise ValueError("đồ thị có trọng số: them_canh(u, v, w)")
        self._ke[u].append((v, w))
        self._co_canh.add((u, v))
        if not self.co_huong:
            self._ke[v].append((u, w))
            self._co_canh.add((v, u))
        self._canh.append([u, v, w])
        self._ghi({"t": "edge", "a": u, "b": v, "x": w})

    def ke(self, u):
        """Duyệt các đỉnh kề u (theo thứ tự thêm cạnh). Mỗi bước duyệt ghi một lần xét cạnh."""
        self._kiem(u)
        self._ghi({"t": "read", "id": u})
        return self._duyet(u)

    def _duyet(self, u):
        for v, w in list(self._ke[u]):
            self._ghi({"t": "read", "a": u, "b": v})
            yield (v, w) if self.co_trong_so else v

    def bac(self, u):
        """Số cạnh đi ra từ u (không ghi sự kiện)."""
        self._kiem(u)
        return len(self._ke[u])

    def danh_dau(self, u, mau="xong"):
        self._kiem(u)
        _kiem_mau(mau)
        self._mau[u] = mau
        self._ghi({"t": "mark", "id": u, "m": mau})

    def danh_dau_canh(self, u, v, mau="xong"):
        _kiem_mau(mau)
        if (u, v) not in self._co_canh:
            raise ValueError(f"không có cạnh {u}–{v}")
        self._mau_canh[self._khoa(u, v)] = mau
        self._ghi({"t": "mark", "a": u, "b": v, "m": mau})

    def nhan(self, u, chu):
        """Chữ nhỏ dưới đỉnh u (khoảng cách, thứ tự thăm…); None để xoá."""
        self._kiem(u)
        chu = None if chu is None else str(chu)
        self._nhan[u] = chu
        self._ghi({"t": "set", "id": u, "x": chu})

    def vi_tri(self, u, x, y):
        self._kiem(u)
        self._vt[u] = [float(x), float(y)]
        self._ghi({"t": "pos", "id": u, "x": float(x), "y": float(y)})

    def _khoa(self, u, v):
        return (u, v) if self.co_huong or u <= v else (v, u)

    def _kiem(self, u):
        if u not in self._ke:
            raise IndexError(f"đỉnh {u} không có trong đồ thị {self.ten}")

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def trang_thai(self):
        return {
            "kieu": "do-thi",
            "co_huong": self.co_huong,
            "dinh": list(self._dinh),
            "canh": [list(c) for c in self._canh],
            "mau": [[u, m] for u, m in self._mau.items() if m is not None],
            "mau_canh": [[a, b, m] for (a, b), m in self._mau_canh.items() if m is not None],
            "nhan": [[u, t] for u, t in self._nhan.items() if t is not None],
            "vt": [[u, x, y] for u, (x, y) in self._vt.items()],
        }

    def __repr__(self):
        return f"DoThi({self.so_dinh} đỉnh, {len(self._canh)} cạnh)"


def _kiem_mau(mau):
    if mau is not None and mau not in MAU:
        raise ValueError(f"mau phải là một trong {MAU}")
