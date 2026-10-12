"""Chạy MỘT test của bài nộp, gọi từ cham.py — mỗi test một tiến trình mới.

    python chay_mot.py <file code>

stdin của tiến trình là input của test, stdout là output của bài làm, nguyên văn: không bọc JSON, nên code
học viên không có cách nào in giả "kết quả" — cái nó in ra CHÍNH LÀ kết quả. Lỗi (traceback rút gọn) ra
stderr, mã thoát 1.

Không ghi trace: `viz.Mang` được thay bằng list thường (cùng chữ ký) để bài O(n²) đúng không bị chậm vì
mỗi lần đọc a[i] phải qua một lời gọi Python.
"""

import math
import os
import sys


class MangNhanh(list):
    """viz.Mang khi chấm: list thường, không ghi sự kiện."""

    def __init__(self, values, ten="a"):
        super().__init__(values)
        self.ten = ten

    def con_tro(self, ten, i):
        pass


def _khong_lam_gi(*args, **kwargs):
    pass


def main():
    # newline="\n": trên Windows (chế độ không sandbox) print không đổi thành \r\n
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    sys.stdin.reconfigure(encoding="utf-8")
    try:
        import resource  # chỉ có trên Linux (trong Docker)

        giay = max(1, math.ceil(int(os.environ.get("LABCAST_TIME_LIMIT_MS", "1000")) / 1000)) + 1
        resource.setrlimit(resource.RLIMIT_CPU, (giay, giay + 1))
    except ImportError:
        pass

    import viz

    viz.Mang = MangNhanh
    viz.say = _khong_lam_gi
    viz.key = _khong_lam_gi

    with open(sys.argv[1], encoding="utf-8") as f:
        code = f.read()
    try:
        exec(compile(code, "bai_lam.py", "exec"), {"__name__": "__main__"})
    except SystemExit:
        raise
    except BaseException as e:  # noqa: BLE001 — mọi lỗi của bài làm đều báo về
        from run import loi_rut_gon

        sys.stdout.flush()
        sys.stderr.write(loi_rut_gon(e, code) + "\n")
        sys.exit(1)
    sys.stdout.flush()


if __name__ == "__main__":
    main()
