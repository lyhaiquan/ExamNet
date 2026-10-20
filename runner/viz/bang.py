"""Bảng 2 chiều "gắn camera" (spec §7.5, task 3.9) cho quy hoạch động, lưới, ma trận.

`dp[i][j]` đi qua một đối tượng hàng trung gian nên bắt được cả hai chỉ số: đọc → `cell_r`, ghi → `cell_w`.
`dp[i][j] += 1` sinh cả hai. Gán cả hàng hay gán lát cắt cũng sinh `cell_w` cho từng ô, không ô nào đổi lặng lẽ.
Bộ vẽ `luoi` nối các ô vừa đọc tới ô được ghi bằng mũi tên phụ thuộc.
"""

from . import recorder


class Bang:
    """Bảng `hang` × `cot`. `nhan_hang`/`nhan_cot`: nhãn in dọc hai trục (ví dụ hai xâu của bài LCS)."""

    def __init__(self, hang, cot, gia_tri_dau=0, ten="dp", nhan_hang=None, nhan_cot=None):
        self.ten = ten
        self._o = [[gia_tri_dau] * cot for _ in range(hang)]
        self._da_ghi, self._danh_dau = set(), set()
        self.nhan_hang = None if nhan_hang is None else [str(x) for x in nhan_hang]
        self.nhan_cot = None if nhan_cot is None else [str(x) for x in nhan_cot]
        recorder.current.register(ten, self)

    @classmethod
    def tu(cls, luoi, ten="g", nhan_hang=None, nhan_cot=None):
        """Bảng có sẵn dữ liệu, ví dụ lưới mê cung đọc từ input. Mọi hàng phải dài bằng nhau."""
        luoi = [list(h) for h in luoi]
        cot = len(luoi[0]) if luoi else 0
        if any(len(h) != cot for h in luoi):
            raise ValueError("Bang.tu: các hàng phải dài bằng nhau")
        b = cls.__new__(cls)
        b.ten = ten
        b._o = luoi
        b._da_ghi, b._danh_dau = set(), set()
        b.nhan_hang = None if nhan_hang is None else [str(x) for x in nhan_hang]
        b.nhan_cot = None if nhan_cot is None else [str(x) for x in nhan_cot]
        recorder.current.register(ten, b)
        return b

    @property
    def so_hang(self):
        return len(self._o)

    @property
    def so_cot(self):
        return len(self._o[0]) if self._o else 0

    def __len__(self):
        return len(self._o)

    def __getitem__(self, i):
        if isinstance(i, tuple):  # dp[i, j]
            r, c = i
            return self[r][c]
        if not isinstance(i, int):
            raise TypeError("Bang chỉ lấy được từng hàng: dp[i] rồi dp[i][j]")
        self._o[i]  # chỉ số sai thì IndexError như list
        return _Hang(self, i % len(self._o))

    def __setitem__(self, i, hang):
        if isinstance(i, tuple):
            r, c = i
            self[r][c] = hang
            return
        h = self[i]
        moi = list(hang)
        if len(moi) != self.so_cot:
            raise ValueError(f"Gán cả hàng phải đủ {self.so_cot} ô")
        for j, x in enumerate(moi):
            h[j] = x

    def __iter__(self):
        for i in range(len(self._o)):
            yield _Hang(self, i)

    def danh_dau(self, i, j):
        """Tô xanh lá ô (i, j) và giữ màu, ví dụ ô trên đường truy vết. Không đổi giá trị."""
        self._o[i][j]
        i, j = i % len(self._o), j % self.so_cot
        self._danh_dau.add((i, j))
        recorder.current.emit({"t": "mark", "v": self.ten, "i": i, "j": j})

    def __repr__(self):
        return f"Bang({self._o!r})"

    def trang_thai(self):
        s = {"kieu": "luoi", "o": [list(h) for h in self._o]}
        if self.nhan_hang is not None:
            s["hang"] = self.nhan_hang
        if self.nhan_cot is not None:
            s["cot"] = self.nhan_cot
        # ô đã ghi và ô đã đánh dấu: tua tới giữa trace vẫn tô đúng màu như khi xem liền
        if self._da_ghi:
            s["ghi"] = [list(c) for c in sorted(self._da_ghi)]
        if self._danh_dau:
            s["dau"] = [list(c) for c in sorted(self._danh_dau)]
        return s


class _Hang:
    """Một hàng của Bang: dp[i] trả về đối tượng này để dp[i][j] biết cả i lẫn j."""

    __slots__ = ("_b", "_i")

    def __init__(self, b, i):
        self._b, self._i = b, i

    def __getitem__(self, j):
        hang = self._b._o[self._i]
        if isinstance(j, slice):
            return [self[k] for k in range(*j.indices(len(hang)))]
        x = hang[j]
        recorder.current.emit({"t": "cell_r", "v": self._b.ten, "i": self._i, "j": j % len(hang)})
        return x

    def __setitem__(self, j, x):
        hang = self._b._o[self._i]
        if isinstance(j, slice):
            chi_so = list(range(*j.indices(len(hang))))
            moi = list(x)
            if len(moi) != len(chi_so):
                raise ValueError("Gán lát cắt phải giữ nguyên số ô")
            for k, v in zip(chi_so, moi):
                self[k] = v
            return
        hang[j] = x
        j = j % len(hang)
        self._b._da_ghi.add((self._i, j))
        recorder.current.emit({"t": "cell_w", "v": self._b.ten, "i": self._i, "j": j, "x": x})

    def __len__(self):
        return len(self._b._o[self._i])

    def __iter__(self):
        for j in range(len(self)):
            yield self[j]

    def __repr__(self):
        return repr(self._b._o[self._i])
