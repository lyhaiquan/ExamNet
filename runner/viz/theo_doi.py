"""Theo dõi code bài làm từng dòng và từng lời gọi hàm (spec §7.5, cách ghi thứ 3: `sys.settrace`).

Cấu trúc nào cần thì đăng ký móc: danh sách liên kết xem biến cục bộ nào đang trỏ vào nút nào sau mỗi dòng;
cây lời gọi ghi `call`/`ret` của các hàm khai báo. Chỉ bật khi đang ghi trace (test nhỏ, xem animation):
chạy test lớn để chấm thì không theo dõi gì, code học viên chạy đủ nhanh như thường.

Chỉ theo dõi khung của `bai_lam.py` (tên file run.py đặt cho code học viên), không đụng code của thư viện.
"""

import sys

from . import recorder

FILE = "bai_lam.py"

_moc_dong = []  # gọi với (frame) sau mỗi dòng
_moc_goi = []  # gọi với ("call" | "return", frame, giá trị trả về)
_da_cai = False


def them_moc_dong(fn):
    _moc_dong.append(fn)
    _cai()


def them_moc_goi(fn):
    _moc_goi.append(fn)
    _cai()


def tat():
    """Gỡ hết móc và thôi theo dõi (run.py gọi khi chạy xong; test gọi giữa các ca)."""
    global _da_cai
    sys.settrace(None)
    _moc_dong.clear()
    _moc_goi.clear()
    _da_cai = False


def _cai():
    global _da_cai
    if _da_cai or not recorder.current.enabled:
        return
    _da_cai = True
    sys.settrace(_toan_cuc)
    # settrace chỉ có hiệu lực với khung mới: gắn tay vào các khung bai_lam.py đang chạy dở (code cấp module
    # đang tạo cấu trúc, hàm đang gọi tới đây…)
    f = sys._getframe(1)
    while f is not None:
        if f.f_code.co_filename == FILE:
            f.f_trace = _cuc_bo
            f.f_trace_lines = True
        f = f.f_back


def _toan_cuc(frame, event, arg):
    if frame.f_code.co_filename != FILE:
        return None
    if event == "call" and not recorder.current.dang_chup:
        for m in _moc_goi:
            m("call", frame, None)
    return _cuc_bo


def _cuc_bo(frame, event, arg):
    if recorder.current.dang_chup:
        return _cuc_bo
    if event == "line":
        for m in _moc_dong:
            m(frame)
    elif event == "return":
        for m in _moc_goi:
            m("return", frame, arg)
    return _cuc_bo
