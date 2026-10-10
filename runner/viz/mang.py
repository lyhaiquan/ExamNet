"""Mảng "gắn camera" (spec §7.5): ghi mọi lần đọc và ghi theo chỉ số."""

from . import recorder


class Mang(list):
    """Dùng như list thường. a[i] sinh sự kiện read, a[i] = x sinh sự kiện set."""

    def __init__(self, values, ten="a"):
        super().__init__(values)
        self.ten = ten
        recorder.current.register(ten, self)

    def __getitem__(self, i):
        if isinstance(i, int) and len(self) > 0:
            recorder.current.emit({"t": "read", "v": self.ten, "i": i % len(self)})
        return super().__getitem__(i)

    def __setitem__(self, i, x):
        super().__setitem__(i, x)
        if isinstance(i, int) and len(self) > 0:
            recorder.current.emit({"t": "set", "v": self.ten, "i": i % len(self), "x": x})

    def con_tro(self, ten, i):
        """Vẽ con trỏ tên `ten` (ví dụ "i", "j") trỏ tới ô i."""
        recorder.current.emit({"t": "ptr", "v": self.ten, "n": ten, "i": i})
