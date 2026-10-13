"""Bộ ghi trace (spec §7.3): mỗi sự kiện một dòng JSON."""

import json


class TraceLimit(Exception):
    """Vượt trần số sự kiện hoặc dung lượng — dừng chương trình học viên."""


class Recorder:
    SNAP_EVERY = 200

    def __init__(self, out, max_events=10_000, max_bytes=2_000_000, enabled=True):
        self.out, self.max_events, self.max_bytes, self.enabled = out, max_events, max_bytes, enabled
        self.count, self.bytes, self.structures = 0, 0, {}
        # Đang chụp trạng thái: trang_thai() có thể chạy code của bài làm (ví dụ hàm học viên viết); bộ theo dõi
        # từng dòng không được ghi sự kiện chen vào giữa, nếu không ảnh chụp lệch với chuỗi sự kiện
        self.dang_chup = False

    def register(self, name, structure):
        """Cấu trúc gắn camera tự đăng ký khi tạo, để keyframe chụp được trạng thái của nó."""
        self.structures[name] = structure
        if self.enabled:
            self._write({"t": "snap", "n": self.count, "state": self._state()})

    def emit(self, event):
        if not self.enabled:
            return
        self._write(event)
        self.count += 1
        if self.count % self.SNAP_EVERY == 0:
            self._write({"t": "snap", "n": self.count, "state": self._state()})

    def _state(self):
        # Cấu trúc có trạng thái riêng (ngăn xếp, bảng 2 chiều…) tự mô tả qua trang_thai(); mảng thì là list
        self.dang_chup = True
        try:
            return {
                name: s.trang_thai() if hasattr(s, "trang_thai") else list(s)
                for name, s in self.structures.items()
            }
        finally:
            self.dang_chup = False

    def _write(self, obj):
        # default=str: học viên đẩy vào một giá trị lạ (object, set…) thì ghi dạng chữ, không làm hỏng bài chạy
        line = json.dumps(obj, ensure_ascii=False, default=str)
        self.bytes += len(line) + 1
        if self.count >= self.max_events or self.bytes > self.max_bytes:
            raise TraceLimit()
        self.out.write(line + "\n")


current = Recorder(out=None, enabled=False)  # run.py thay bằng bộ ghi thật


def say(text):
    """Lời thuyết minh ngắn (≤ 12 từ) hiện dưới animation."""
    current.emit({"t": "say", "s": str(text)})


def key(text):
    """Bước quan trọng: trình phát có nút nhảy tới bước quan trọng kế tiếp."""
    current.emit({"t": "key", "s": str(text)})
