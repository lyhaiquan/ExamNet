"""Ngăn xếp và hàng đợi "gắn camera" (spec §7.5, task 3.9).

Không kế thừa `list`: mọi cách đổi dữ liệu đều đi qua các hàm dưới đây và đều sinh sự kiện, nên hoạt hình
không bao giờ lệch với dữ liệu thật (kế thừa `list` thì `sort`, `append`… đổi dữ liệu mà không ai ghi).
Có sẵn tên quen của `list`/`deque` (`append`, `pop`, `popleft`, `st[-1]`, `q[0]`) để học viên viết như thường.
"""

from collections import deque

from . import recorder


class NganXep:
    """Ngăn xếp: `push`/`append` → sự kiện `push`, `pop` → `pop`, xem đỉnh (`top()`, `st[-1]`) → `read`."""

    def __init__(self, gia_tri=(), ten="st"):
        self.ten = ten
        self._ds = list(gia_tri)
        recorder.current.register(ten, self)

    def push(self, x):
        self._ds.append(x)
        recorder.current.emit({"t": "push", "v": self.ten, "x": x})

    append = push

    def pop(self):
        if not self._ds:
            raise IndexError("pop() trên ngăn xếp rỗng")
        x = self._ds.pop()
        recorder.current.emit({"t": "pop", "v": self.ten, "x": x})
        return x

    def top(self):
        """Phần tử ở đỉnh, không lấy ra."""
        if not self._ds:
            raise IndexError("top() trên ngăn xếp rỗng")
        return self[-1]

    def rong(self):
        return not self._ds

    def __getitem__(self, i):
        x = self._ds[i]  # chỉ số sai thì IndexError như list
        recorder.current.emit({"t": "read", "v": self.ten, "i": i % len(self._ds)})
        return x

    def __len__(self):
        return len(self._ds)

    def __bool__(self):
        return bool(self._ds)

    def __iter__(self):
        # duyệt từ đáy lên đỉnh, không ghi sự kiện (chỉ đọc cả ngăn xếp, ví dụ để in)
        return iter(list(self._ds))

    def __repr__(self):
        return f"NganXep({self._ds!r})"

    def trang_thai(self):
        return {"kieu": "ngan-xep", "ds": list(self._ds)}


class HangDoi:
    """Hàng đợi: `enq`/`append` → sự kiện `enq` (vào cuối), `deq`/`popleft` → `deq` (ra đầu), xem đầu → `read`."""

    def __init__(self, gia_tri=(), ten="q"):
        self.ten = ten
        self._ds = deque(gia_tri)
        recorder.current.register(ten, self)

    def enq(self, x):
        self._ds.append(x)
        recorder.current.emit({"t": "enq", "v": self.ten, "x": x})

    append = enq

    def deq(self):
        if not self._ds:
            raise IndexError("deq() trên hàng đợi rỗng")
        x = self._ds.popleft()
        recorder.current.emit({"t": "deq", "v": self.ten, "x": x})
        return x

    popleft = deq

    def dau(self):
        """Phần tử ở đầu hàng, không lấy ra."""
        if not self._ds:
            raise IndexError("dau() trên hàng đợi rỗng")
        return self[0]

    def rong(self):
        return not self._ds

    def __getitem__(self, i):
        x = self._ds[i]
        recorder.current.emit({"t": "read", "v": self.ten, "i": i % len(self._ds)})
        return x

    def __len__(self):
        return len(self._ds)

    def __bool__(self):
        return bool(self._ds)

    def __iter__(self):
        return iter(list(self._ds))

    def __repr__(self):
        return f"HangDoi({list(self._ds)!r})"

    def trang_thai(self):
        return {"kieu": "hang-doi", "ds": list(self._ds)}
