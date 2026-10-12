"""Kiểm hàm cấm bằng ast TRƯỚC khi chạy bài nộp (spec §8.2, §8.4). Không chạy code học viên.

Bài cấm `sorted` / `sort` thì bắt cả gọi lẫn lấy tên ra dùng gián tiếp:

    sorted(a)   a.sort()   f = sorted; f(a)   list.sort(a)   getattr(a, "sort")()

Khi bài có danh sách cấm, `eval`, `exec`, `compile`, `__import__` và `__builtins__` cũng bị chặn: đó là các
đường né kiểm tra bằng chuỗi. Vẫn có cách lách (spec §19 mục 5); luật soi trace (luat.py) là lớp thứ hai.

Hàm tự viết trùng tên (`def sort(a): ...`) là code của học viên, không bị bắt.
"""

import ast

NE_KIEM_TRA = ("eval", "exec", "compile", "__import__", "__builtins__")
# Lấy lại hàm cấm dưới tên khác: from builtins import sorted as s, importlib.import_module(...)
IMPORT_NE_KIEM_TRA = ("builtins", "importlib")


def _ten_tu_dinh_nghia(cay):
    """Tên học viên tự định nghĩa: hàm, lớp, biến được gán."""
    ten = set()
    for nut in ast.walk(cay):
        if isinstance(nut, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            ten.add(nut.name)
        elif isinstance(nut, ast.Name) and isinstance(nut.ctx, ast.Store):
            ten.add(nut.id)
        elif isinstance(nut, ast.arg):
            ten.add(nut.arg)
    return ten


def kiem(code, cam_dung=(), cam_import=(), cam_cu_phap=()):
    """Trả danh sách lỗi (chuỗi tiếng Việt, có số dòng), rỗng nếu không vi phạm.

    Code lỗi cú pháp thì trả rỗng: để lần chạy báo lỗi cú pháp như thường, không coi là dùng hàm cấm.
    """
    cam_dung, cam_import, cam_cu_phap = set(cam_dung), set(cam_import), set(cam_cu_phap)
    if not (cam_dung or cam_import or cam_cu_phap):
        return []
    try:
        cay = ast.parse(code)
    except SyntaxError:
        return []
    tu_dinh_nghia = _ten_tu_dinh_nghia(cay)
    loi = []

    def bao(nut, noi_dung):
        dong = getattr(nut, "lineno", None)
        thong_bao = noi_dung + (f" (dòng {dong})" if dong else "")
        if thong_bao not in loi:
            loi.append(thong_bao)

    co_danh_sach_cam = bool(cam_dung or cam_import)
    for nut in ast.walk(cay):
        if isinstance(nut, ast.Name) and isinstance(nut.ctx, ast.Load):
            if nut.id in cam_dung and nut.id not in tu_dinh_nghia:
                bao(nut, f"Không được dùng {nut.id}")
            elif co_danh_sach_cam and nut.id in NE_KIEM_TRA:
                bao(nut, f"Không được dùng {nut.id} trong bài này")
        elif isinstance(nut, ast.Attribute):
            if nut.attr in cam_dung and nut.attr not in tu_dinh_nghia:
                bao(nut, f"Không được dùng .{nut.attr}()")
        elif isinstance(nut, ast.Call) and isinstance(nut.func, ast.Name) and nut.func.id == "getattr":
            for doi in nut.args[1:2]:
                if isinstance(doi, ast.Constant) and doi.value in cam_dung:
                    bao(nut, f"Không được dùng {doi.value}")
        elif isinstance(nut, ast.Import):
            for ten in nut.names:
                goc = ten.name.split(".")[0]
                if ten.name in cam_import or goc in cam_import:
                    bao(nut, f"Không được import {ten.name}")
                elif co_danh_sach_cam and goc in IMPORT_NE_KIEM_TRA:
                    bao(nut, f"Không được import {ten.name} trong bài này")
        elif isinstance(nut, ast.ImportFrom):
            goc = nut.module or ""
            if goc in cam_import or goc.split(".")[0] in cam_import:
                bao(nut, f"Không được import {goc}")
            elif co_danh_sach_cam and goc.split(".")[0] in IMPORT_NE_KIEM_TRA:
                # from builtins import sorted as s: đổi tên thì kiểm theo tên không bắt được
                bao(nut, f"Không được import từ {goc} trong bài này")
        if type(nut).__name__ in cam_cu_phap:
            bao(nut if hasattr(nut, "lineno") else _cha_co_dong(cay, nut), _ten_cu_phap(type(nut).__name__))
    return loi


def _cha_co_dong(cay, con):
    """Toán tử (ast.Pow…) không có số dòng: lấy số dòng của biểu thức chứa nó."""
    for nut in ast.walk(cay):
        for c in ast.iter_child_nodes(nut):
            if c is con and hasattr(nut, "lineno"):
                return nut
    return con


def _ten_cu_phap(kieu):
    return {
        "Pow": "Không được dùng toán tử **",
        "Dict": "Không được dùng dict {k: v}",
        "DictComp": "Không được dùng dict comprehension",
        "Set": "Không được dùng set {x}",
        "SetComp": "Không được dùng set comprehension",
    }.get(kieu, f"Không được dùng cú pháp {kieu}")
