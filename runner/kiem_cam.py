"""Kiểm hàm cấm bằng ast TRƯỚC khi chạy bài nộp (spec §8.2, §8.4). Không chạy code học viên.

Bài cấm `sorted` / `sort` thì bắt cả gọi lẫn lấy tên ra dùng gián tiếp:

    sorted(a)   a.sort()   f = sorted; f(a)   list.sort(a)   getattr(a, "sort")()

Khi bài có danh sách cấm, `eval`, `exec`, `compile`, `__import__` và `__builtins__` cũng bị chặn: đó là các
đường né kiểm tra bằng chuỗi. Vẫn có cách lách (spec §19 mục 5); luật soi trace (luat.py) là lớp thứ hai.

Hàm tự viết trùng tên (`def sort(a): ...`) là code của học viên, không bị bắt.
"""

import ast
import re

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


# ---- C, C++, Java (spec đa ngôn ngữ §2) ------------------------------------------------------------------

# Bài chỉ khai báo hàm cấm cho Python (cam_dung, cam_import) mà không có cam_theo_ngon_ngu: suy ra hàm tương
# đương ở ngôn ngữ khác, để "không dùng sort" ở Python cũng là "không dùng std::sort" ở C++.
# Tên bắt đầu bằng "." là phương thức của đối tượng bất kỳ (list.sort(), s.indexOf()).
TUONG_DUONG = {
    "sort": {"c": ["qsort"], "cpp": ["sort", "stable_sort", "partial_sort", "nth_element", "qsort"],
             "java": ["Arrays.sort", "Collections.sort", ".sort", ".sorted"]},
    "sorted": {"c": ["qsort"], "cpp": ["sort", "stable_sort", "partial_sort", "nth_element", "qsort"],
               "java": ["Arrays.sort", "Collections.sort", ".sort", ".sorted"]},
    "min": {"cpp": ["min_element", "minmax_element"], "java": ["Collections.min", ".min"]},
    "max": {"cpp": ["max_element", "minmax_element"], "java": ["Collections.max", ".max"]},
    "reversed": {"cpp": ["reverse"], "java": ["Collections.reverse", ".reverse"]},
    "index": {"c": ["strchr", "strstr", "memchr"], "cpp": ["find", "find_if", ".find"],
              "java": [".indexOf", ".lastIndexOf"]},
    "find": {"c": ["strchr", "strstr", "memchr"], "cpp": ["find", "find_if", ".find"],
             "java": [".indexOf", ".lastIndexOf", ".contains"]},
    "count": {"cpp": ["count", "count_if", ".count"], "java": ["Collections.frequency"]},
    "set": {"cpp": ["set", "unordered_set", "multiset"], "java": ["HashSet", "TreeSet", "LinkedHashSet"]},
    "frozenset": {"cpp": ["set", "unordered_set"], "java": ["HashSet", "TreeSet"]},
    "dict": {"cpp": ["map", "unordered_map", "multimap"],
             "java": ["HashMap", "TreeMap", "LinkedHashMap", "Hashtable"]},
    "bin": {"cpp": ["bitset"], "java": ["Integer.toBinaryString", "Long.toBinaryString", "Integer.toString"]},
    "format": {"cpp": ["bitset"], "java": ["Integer.toBinaryString", "Long.toBinaryString"]},
}
TUONG_DUONG_IMPORT = {
    "heapq": {"cpp": ["priority_queue", "make_heap", "push_heap", "pop_heap"], "java": ["PriorityQueue"]},
    "bisect": {"cpp": ["lower_bound", "upper_bound", "binary_search", "equal_range"],
               "java": ["Arrays.binarySearch", "Collections.binarySearch"]},
    "collections": {"cpp": ["deque", "queue", "list"], "java": ["ArrayDeque", "LinkedList", "Deque", "Queue"]},
    "queue": {"cpp": ["deque", "queue"], "java": ["ArrayDeque", "LinkedList", "Deque", "Queue"]},
    "itertools": {"cpp": ["next_permutation", "prev_permutation"]},
}

_CHU_THICH_CHUOI = re.compile(
    r"//[^\n]*|/\*.*?\*/|\"(?:\\.|[^\"\\\n])*\"|'(?:\\.|[^'\\\n])*'", re.S)


def _bo_chu_thich_chuoi(code):
    """Thay chú thích và chuỗi bằng dấu cách (giữ xuống dòng để số dòng vẫn đúng)."""
    return _CHU_THICH_CHUOI.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), code)


def cam_cua(ngon_ngu, cam_dung=(), cam_import=(), cam_theo_ngon_ngu=None):
    """Danh sách tên cấm cho ngôn ngữ: khai báo riêng của bài nếu có, không thì suy từ cấm của Python."""
    if cam_theo_ngon_ngu and ngon_ngu in cam_theo_ngon_ngu:
        return list(cam_theo_ngon_ngu[ngon_ngu])
    ds = []
    for ten in cam_dung:
        ds += TUONG_DUONG.get(ten, {}).get(ngon_ngu, [])
    for ten in cam_import:
        ds += TUONG_DUONG_IMPORT.get(ten, {}).get(ngon_ngu, [])
    return list(dict.fromkeys(ds))


def kiem_ngon_ngu(code, ngon_ngu, cam):
    """Kiểm thô bằng tên (sau khi bỏ chú thích, chuỗi). Trả danh sách lỗi có số dòng, rỗng nếu không vi phạm.

    Hàm học viên tự định nghĩa trùng tên (`void sort(int a[], int n) { … }`) không bị bắt. Đổi tên qua
    `#define` hay con trỏ hàm vẫn lách được: bài cần chặt thì cho làm bằng Python có luật soi trace.
    """
    if not cam:
        return []
    sach = _bo_chu_thich_chuoi(code)
    tu_dinh_nghia = set(re.findall(r"\b([A-Za-z_]\w*)\s*\([^;{}()]*\)\s*(?:const\s*)?\{", sach))
    tu_dinh_nghia |= set(re.findall(r"\b(?:class|struct|interface|enum)\s+([A-Za-z_]\w*)", sach))
    loi = []
    for ten in cam:
        # std::sort cũng bắt sort(...) viết sau using namespace std
        ten = ten[5:] if ten.startswith("std::") else ten
        if ten.startswith("."):
            mau = r"\.\s*" + re.escape(ten[1:]) + r"\s*\("
            hien_ten = ten + "()"
        elif "." in ten or "::" in ten:
            phan = re.split(r"\.|::", ten)
            mau = r"\b" + r"\s*(?:\.|::)\s*".join(re.escape(p) for p in phan) + r"\b"
            hien_ten = ten
        else:
            if ten in tu_dinh_nghia:
                continue
            mau = r"(?<![\w.])(?:std\s*::\s*)?" + re.escape(ten) + r"\b(?!\s*::)"
            hien_ten = ten
        m = re.search(mau, sach)
        if m:
            dong = sach.count("\n", 0, m.start()) + 1
            loi.append(f"Không được dùng {hien_ten} (dòng {dong})")
    return loi
