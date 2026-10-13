"""Bảng băm "gắn camera" (spec §7.5): dây chuyền hoặc địa chỉ mở.

Dây chuyền (mặc định): mỗi ô `T[h]` là một danh sách nhỏ.

    T = viz.BangBam(7, ten="T")
    T[h].append(x)        # → bucket op "add"
    x in T[h]             # → read từng phần tử đã so (k là vị trí trong dây chuyền), dừng khi gặp
    T[h].remove(x)        # → bucket op "del"

Địa chỉ mở (`mo=True`): mỗi ô giữ một giá trị hoặc None (trống).

    T = viz.BangBam(7, mo=True, ten="T")
    T[h]                  # → read ô h (dò tới ô nào thấy ô đó sáng lên)
    T[h] = x              # → set; T[h] = viz.XOA đánh dấu ô đã xoá (vẫn dò tiếp qua được)
"""

from . import recorder


class _DaXoa:
    """Dấu "đã xoá" của địa chỉ mở: khác None (ô trống) vì khi tìm vẫn phải dò tiếp qua nó."""

    def __repr__(self):
        return "XOA"

    __str__ = __repr__


XOA = _DaXoa()


class BangBam:
    def __init__(self, m, mo=False, ten="T"):
        if m < 1:
            raise ValueError("bảng băm cần ít nhất 1 ô")
        self.ten = ten
        self.mo = mo
        self._o = [None] * m if mo else [[] for _ in range(m)]
        recorder.current.register(ten, self)

    def __len__(self):
        return len(self._o)

    def __getitem__(self, h):
        if not isinstance(h, int):
            raise TypeError("chỉ số ô phải là số nguyên")
        x = self._o[h]  # chỉ số sai thì IndexError như list
        h %= len(self._o)
        if self.mo:
            self._ghi({"t": "read", "i": h})
            return x
        return _DayChuyen(self, h)

    def __setitem__(self, h, x):
        if not self.mo:
            raise TypeError("bảng dây chuyền: thêm bằng T[h].append(x), không gán cả ô")
        self._o[h] = x
        self._ghi({"t": "set", "i": h % len(self._o), "x": x})

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def trang_thai(self):
        return {"kieu": "bam", "mo": self.mo, "o": [list(c) for c in self._o] if not self.mo else list(self._o)}

    def __repr__(self):
        return f"BangBam({self._o!r})"


class _DayChuyen:
    """Dây chuyền ở ô h. Mọi cách đọc, đổi đều ghi lại; không kế thừa list để không có đường đổi lặng lẽ."""

    def __init__(self, bang, h):
        self._b, self._h = bang, h

    @property
    def _ds(self):
        return self._b._o[self._h]

    def _ghi(self, op, **kw):
        self._b._ghi({"t": "bucket", "i": self._h, "op": op, **kw})

    def append(self, x):
        self._ds.append(x)
        self._ghi("add", x=x, k=len(self._ds) - 1)

    def remove(self, x):
        for k, y in enumerate(self._ds):
            self._ghi("read", k=k)
            if y == x:
                del self._ds[k]
                self._ghi("del", k=k)
                return
        raise ValueError(f"{x!r} không có trong ô {self._h}")

    def pop(self, k=-1):
        n = len(self._ds)
        x = self._ds.pop(k)
        self._ghi("del", k=k % n)
        return x

    def __contains__(self, x):
        for k, y in enumerate(self._ds):
            self._ghi("read", k=k)
            if y == x:
                return True
        return False

    def __iter__(self):
        for k in range(len(self._ds)):
            self._ghi("read", k=k)
            yield self._ds[k]

    def __getitem__(self, k):
        x = self._ds[k]
        self._ghi("read", k=k % len(self._ds))
        return x

    def __setitem__(self, k, x):
        self._ds[k] = x
        self._ghi("set", k=k % len(self._ds), x=x)

    def __len__(self):
        return len(self._ds)

    def __bool__(self):
        return bool(self._ds)

    def __repr__(self):
        return repr(self._ds)
