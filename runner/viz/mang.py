"""Mảng "gắn camera" (spec §7.5): ghi mọi lần đọc và ghi theo chỉ số.

Kế thừa `list` để học viên dùng như list thường, nên MỌI hàm đổi dữ liệu của `list` đều phải ghi lại, nếu không
hình vẽ lệch với dữ liệu thật: `append` → `push`, `pop` → `pop`, `insert` → `ins`; các hàm đổi nhiều ô một lúc
(`sort`, `reverse`, `extend`, `remove`, `clear`, `del`, gán lát cắt, `+=`, `*=`) ghi `all` kèm cả mảng mới.

Hàm viết bằng C nhận list (ví dụ `heapq.heappush`) đổi thẳng dữ liệu bên trong, không đi qua các hàm này, nên
không ghi được: bài dùng mảng để vẽ heap thì cấm `heapq` (`cam_import`).
"""

from . import recorder


class Mang(list):
    """Dùng như list thường. a[i] sinh sự kiện read, a[i] = x sinh sự kiện set."""

    def __init__(self, values, ten="a"):
        super().__init__(values)
        self.ten = ten
        recorder.current.register(ten, self)

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def _ca_mang(self):
        self._ghi({"t": "all", "ds": list(self)})

    def __getitem__(self, i):
        if isinstance(i, int) and len(self) > 0:
            recorder.current.emit({"t": "read", "v": self.ten, "i": i % len(self)})
        return super().__getitem__(i)

    def __setitem__(self, i, x):
        super().__setitem__(i, x)
        if isinstance(i, slice):
            self._ca_mang()
        elif len(self) > 0:
            recorder.current.emit({"t": "set", "v": self.ten, "i": i % len(self), "x": x})

    def __delitem__(self, i):
        super().__delitem__(i)
        self._ca_mang()

    def __iadd__(self, other):
        super().__iadd__(other)
        self._ca_mang()
        return self

    def __imul__(self, n):
        super().__imul__(n)
        self._ca_mang()
        return self

    def append(self, x):
        super().append(x)
        self._ghi({"t": "push", "x": x})

    def pop(self, i=-1):
        n = len(self)
        x = super().pop(i)  # mảng rỗng hay chỉ số sai thì IndexError như list
        self._ghi({"t": "pop", "i": i % n, "x": x})
        return x

    def insert(self, i, x):
        n = len(self)
        # như list.insert: chỉ số âm tính từ cuối, quá hai đầu thì chèn vào đầu/cuối
        vi_tri = max(0, min(n, i + n if i < 0 else i))
        super().insert(i, x)
        self._ghi({"t": "ins", "i": vi_tri, "x": x})

    def extend(self, it):
        super().extend(it)
        self._ca_mang()

    def remove(self, x):
        super().remove(x)
        self._ca_mang()

    def clear(self):
        super().clear()
        self._ca_mang()

    def sort(self, *args, **kwargs):
        super().sort(*args, **kwargs)
        self._ca_mang()

    def reverse(self):
        super().reverse()
        self._ca_mang()

    def con_tro(self, ten, i):
        """Vẽ con trỏ tên `ten` (ví dụ "i", "j") trỏ tới ô i."""
        recorder.current.emit({"t": "ptr", "v": self.ten, "n": ten, "i": i})
