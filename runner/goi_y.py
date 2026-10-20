"""Gợi ý tiếng Việt cho lỗi trong bài làm: học viên đọc ở khung output, app tô đỏ đúng dòng lỗi.

`thong_tin(exc, code)` trả {"n": dòng, "kieu": tên lỗi, "s": thông điệp gốc, "goi_y": câu gợi ý hoặc ""}.
Gợi ý chỉ nói lỗi thuộc loại gì và nên xem chỗ nào, không sửa hộ.
"""

import builtins
import difflib
import re
import traceback

FILE = "bai_lam.py"

# Tên quen tay từ ngôn ngữ khác → cách viết Python
_QUEN_TAY = {
    ("list", "push"): "append",
    ("list", "add"): "append",
    ("list", "length"): "len(...)",
    ("list", "size"): "len(...)",
    ("list", "push_back"): "append",
    ("list", "pop_front"): "pop(0)",
    ("list", "contains"): "x in a",
    ("list", "find"): "index",
    ("str", "length"): "len(...)",
    ("str", "contains"): "x in s",
    ("str", "push"): "s + x",
    ("str", "append"): "s + x (chuỗi không sửa tại chỗ được)",
    ("str", "reverse"): "s[::-1]",
    ("str", "sort"): "sorted(s)",
    ("dict", "has_key"): "k in d",
    ("dict", "contains"): "k in d",
    ("dict", "add"): "d[k] = v",
    ("dict", "push"): "d[k] = v",
    ("set", "append"): "add",
    ("set", "push"): "add",
    ("tuple", "append"): "tuple không sửa được; dùng list",
    ("int", "length"): "len(str(x))",
}

_CU_PHAP = [
    (r"expected ':'", "Thiếu dấu hai chấm ':' ở cuối dòng if/elif/else/for/while/def/class."),
    (r"expected an indented block", "Thân của if/for/while/def phải thụt vào (4 dấu cách) so với dòng trên."),
    (r"unexpected indent", "Dòng này thụt lề thừa: các dòng cùng khối phải thẳng hàng nhau."),
    (r"unindent does not match", "Thụt lề không khớp với khối nào phía trên: kiểm lại số dấu cách."),
    (r"inconsistent use of tabs", "Trộn phím Tab và dấu cách khi thụt lề: dùng một kiểu (4 dấu cách)."),
    (r"was never closed", "Mở ngoặc mà chưa đóng: đếm lại ( [ { và ) ] }."),
    (r"unmatched", "Đóng ngoặc thừa hoặc sai loại ngoặc."),
    (r"does not match opening parenthesis", "Ngoặc đóng không cùng loại với ngoặc mở."),
    (r"unterminated string|EOL while scanning", "Chuỗi thiếu dấu nháy đóng."),
    (r"Maybe you meant '==' or ':='|cannot assign to", "So sánh dùng '==', gán dùng '='. Không gán được vào biểu thức."),
    (r"invalid syntax\. Perhaps you forgot a comma", "Có thể thiếu dấu phẩy giữa các phần tử."),
    (r"invalid decimal literal|invalid character", "Có ký tự lạ hoặc tên biến bắt đầu bằng số."),
    (r"'return' outside function", "return chỉ dùng được bên trong hàm def."),
    (r"'break' outside loop|'continue' (not properly in|outside) loop", "break/continue chỉ dùng trong vòng lặp."),
    (r"expected 'except' or 'finally'", "try phải đi kèm except hoặc finally."),
    (r"invalid syntax", "Câu lệnh viết sai cú pháp Python: xem kỹ dòng này và cuối dòng ngay trên."),
]


def dong_loi(exc):
    """Dòng lỗi trong bai_lam.py (khung sâu nhất), 0 nếu không rõ."""
    if isinstance(exc, SyntaxError):
        return (exc.lineno or 0) if exc.filename == FILE else 0
    n = 0
    for khung in traceback.extract_tb(exc.__traceback__):
        if khung.filename == FILE:
            n = khung.lineno or 0
    return n


def thong_tin(exc, code):
    kieu = type(exc).__name__
    try:
        g = _goi_y(exc, code)
    except Exception:  # noqa: BLE001 — gợi ý hỏng thì thôi, vẫn báo lỗi gốc
        g = ""
    return {"n": dong_loi(exc), "kieu": kieu, "s": str(exc.msg if isinstance(exc, SyntaxError) else exc), "goi_y": g}


def _goi_y(exc, code):
    if isinstance(exc, SyntaxError):
        msg = exc.msg or ""
        for mau, cau in _CU_PHAP:
            if re.search(mau, msg):
                return cau
        return "Câu lệnh viết sai cú pháp Python."
    if isinstance(exc, NameError):
        ten = getattr(exc, "name", None) or _trong_nhay(str(exc))
        gan = difflib.get_close_matches(ten or "", _ten_trong_code(code) + dir(builtins), n=1)
        cau = f"Tên '{ten}' chưa được gán giá trị trước khi dùng, hoặc gõ sai tên."
        return cau + (f" Có phải ý em là '{gan[0]}'?" if gan and gan[0] != ten else "")
    if isinstance(exc, AttributeError):
        obj, ten = getattr(exc, "obj", None), getattr(exc, "name", None)
        if obj is None and "NoneType" in str(exc):
            return ("Biến đang là None. Hay gặp khi gán kết quả của hàm không trả về gì, ví dụ a = a.sort() "
                    "(sort sửa tại chỗ và trả None), hoặc nút cây/danh sách đã hết (None).")
        kieu = type(obj).__name__ if obj is not None else _trong_nhay(str(exc))
        if ten is None:
            return f"Kiểu {kieu} không có thuộc tính hay phương thức này."
        thay = _QUEN_TAY.get((kieu, ten))
        if thay:
            return f"Kiểu {kieu} không có '{ten}'. Trong Python dùng: {thay}."
        cac_ten = [m for m in dir(type(obj)) if not m.startswith("_")] if obj is not None else []
        gan = difflib.get_close_matches(ten, cac_ten, n=1)
        if gan:
            return f"Kiểu {kieu} không có '{ten}'. Có phải ý em là '{gan[0]}'?"
        if cac_ten and len(cac_ten) <= 12:
            return f"Kiểu {kieu} không có '{ten}'. Các phương thức có: {', '.join(cac_ten)}."
        return f"Kiểu {kieu} không có thuộc tính hay phương thức '{ten}'."
    if isinstance(exc, IndexError):
        return ("Chỉ số vượt ngoài mảng/chuỗi: mảng có len(a) phần tử thì chỉ số hợp lệ là 0 … len(a) - 1. "
                "Xem giá trị các biến chỉ số ở bảng biến bước cuối.")
    if isinstance(exc, KeyError):
        return f"Khoá {exc} chưa có trong dict. Dùng d.get(k, mặc_định) hoặc kiểm 'k in d' trước khi đọc."
    if isinstance(exc, ZeroDivisionError):
        return "Chia cho 0: kiểm mẫu số trước khi chia."
    if isinstance(exc, RecursionError):
        return "Đệ quy quá sâu: hàm gọi lại chính nó mãi, có thể thiếu hoặc sai điều kiện dừng."
    if isinstance(exc, EOFError):
        return "Đọc input() nhiều hơn số dòng của test: kiểm lại số lần đọc."
    if isinstance(exc, ValueError):
        s = str(exc)
        if "invalid literal for int()" in s:
            return "int() nhận một chuỗi không phải số nguyên. Có thể quên split() khi đọc nhiều số trên một dòng."
        if "unpack" in s:
            return "Số biến bên trái dấu = không khớp với số giá trị bên phải."
        if "not in list" in s:
            return "list.index/remove không tìm thấy giá trị: kiểm 'x in a' trước."
        return ""
    if isinstance(exc, TypeError):
        s = str(exc)
        bang = [
            ("not subscriptable", "Dùng [ ] trên một giá trị không phải mảng/chuỗi/dict (ví dụ một số hay None)."),
            ("can only concatenate str", "Ghép chuỗi với số: đổi bằng str(x) hoặc dùng f-string."),
            ("must be str, not", "Ghép chuỗi với số: đổi bằng str(x) hoặc dùng f-string."),
            ("unsupported operand", "Phép toán giữa hai kiểu không đi với nhau (ví dụ số + chuỗi, số + None)."),
            ("indices must be integers", "Chỉ số mảng phải là số nguyên: dùng // thay cho / khi chia."),
            ("not callable", "Gọi ( ) trên thứ không phải hàm. Có thể em đặt tên biến trùng tên hàm (vd. len, max, sum)."),
            ("positional argument", "Gọi hàm sai số tham số so với lúc khai báo def."),
            ("missing", "Gọi hàm thiếu tham số."),
            ("not iterable", "Dùng for/in trên một giá trị không lặp được (ví dụ một số: dùng range(n))."),
            ("'<' not supported", "So sánh hai kiểu khác nhau (ví dụ số với chuỗi, hay với None)."),
            ("'>' not supported", "So sánh hai kiểu khác nhau (ví dụ số với chuỗi, hay với None)."),
            ("object does not support item assignment", "Chuỗi và tuple không sửa từng phần tử được: tạo mới hoặc dùng list."),
        ]
        for mau, cau in bang:
            if mau in s:
                return cau
    return ""


def _trong_nhay(s):
    m = re.search(r"'([^']+)'", s)
    return m.group(1) if m else ""


def _ten_trong_code(code):
    return sorted(set(re.findall(r"[A-Za-z_][A-Za-z0-9_]*", code)))
