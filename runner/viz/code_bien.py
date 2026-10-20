"""Chạy từng dòng (spec §7.4 `code-bien`): dòng đang chạy và bảng biến cục bộ, không cần sửa code.

Bài khai báo `- bien: code` / `kieu: code-bien` trong `khung_nhin`; run.py bật bộ ghi kèm nguyên code đang chạy.
Sau mỗi dòng ghi `line` (số dòng); biến cục bộ của khung đang chạy đổi thì ghi `vars` (cả bảng, tên hàm `f`).

Trace của lời giải mẫu chứa nguyên code lời giải, nên chỉ khai báo bộ vẽ này ở bài giảng giải.
"""

import types

from . import recorder, theo_doi

DAI_GIA_TRI = 40
TOI_DA_BIEN = 12


def bat(ten, code, max_lines=None):
    return CodeBien(ten, code, max_lines=max_lines)


def _hien(x):
    s = repr(x)
    return s if len(s) <= DAI_GIA_TRI else s[: DAI_GIA_TRI - 1] + "…"


def _bo_qua(ten, x):
    if ten.startswith("_") or ten == "viz":
        return True
    if isinstance(x, (types.ModuleType, types.FunctionType, types.BuiltinFunctionType, type)):
        return True
    # cấu trúc gắn camera đã có bộ vẽ riêng
    return type(x).__module__.startswith("viz")


class CodeBien:
    def __init__(self, ten, code, max_lines=None):
        self.ten = ten
        self._src = code
        self._max_lines = max_lines
        self._recorded_lines = 0
        self._dong = 0
        self._ham = ""
        self._bien = []
        recorder.current.register(ten, self)
        theo_doi.them_moc_dong(self._moc)

    def _moc(self, frame):
        if self._max_lines is not None and self._recorded_lines >= self._max_lines:
            return
        self._recorded_lines += 1
        self._dong = frame.f_lineno
        self._ghi({"t": "line", "n": self._dong})
        ham = "" if frame.f_code.co_name == "<module>" else frame.f_code.co_name
        bien = [[k, _hien(v)] for k, v in frame.f_locals.items() if not _bo_qua(k, v)][:TOI_DA_BIEN]
        if bien != self._bien or ham != self._ham:
            self._bien, self._ham = bien, ham
            self._ghi({"t": "vars", "f": ham, "d": bien})

    def _ghi(self, su_kien):
        su_kien["v"] = self.ten
        recorder.current.emit(su_kien)

    def trang_thai(self):
        return {"kieu": "code-bien", "src": self._src, "dong": self._dong, "ham": self._ham, "bien": self._bien}
