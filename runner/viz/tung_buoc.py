"""Chạy từng bước cho bài luyện tập (bộ vẽ `tung-dong`): code của chính học viên, không cần sửa code.

Khác `code-bien` (chỉ ở bài giảng giải, trace lời giải mẫu): bộ này bật cho mọi lượt Chạy thử của bài `luyen-tap`,
với test nhỏ xem được. Mỗi dòng sắp chạy ghi một sự kiện

    {"t": "buoc", "v": "buoc", "n": 6, "f": "", "ngan": ["<module>"], "bien": [["j", "0"], ...],
     "o": 12, "dk": {"src": "j >= 0 and a[j] > key", "thay": "0 >= 0 and 3 > 1", "ve": ["True", "True"],
                     "kq": "True"}}

`o` là số ký tự học viên đã in tới lúc đó (app cắt output cuối theo mốc này). `dk` chỉ có ở dòng `if`/`elif`/
`while`/`for`/`assert`: tính lại biểu thức điều kiện như Python sắp tính (đoản mạch `and`/`or`), nhưng chỉ khi
biểu thức không gọi code của học viên (không gọi hàm tự viết, không gọi phương thức). Trả về của hàm ghi
`buoc-ve`. Quá `TRAN` bước thì ghi `buoc-het`, gỡ móc và để chương trình chạy tiếp hết tốc độ.

Trạng thái ghi là TRƯỚC khi dòng chạy, như con trỏ dòng của trình gỡ lỗi.
"""

import ast
import builtins
import sys
import types

from . import recorder

FILE = "bai_lam.py"
TRAN = 500
DAI = 60
TOI_DA_BIEN = 15
SAU_NGAN = 8

_dang = None


def bat(ten, code, tran=TRAN):
    global _dang
    _dang = TungBuoc(ten, code, tran)
    return _dang


def tat():
    """Gỡ móc (run.py gọi khi chạy xong, test gọi giữa các ca)."""
    global _dang
    if _dang is not None:
        _dang.go()
    _dang = None


def ngung():
    """Thôi theo dõi nhưng giữ bộ ghi, để còn ghi dòng lỗi và output cuối."""
    if _dang is not None:
        _dang.go()


def xong(stdout, ok):
    """Ghi output cuối để app hiện output tới từng bước."""
    if _dang is not None:
        _dang.ghi({"t": "buoc-xong", "out": stdout[:20_000], "ok": ok})


# ---------------------------------------------------------------- hiện giá trị


def _hien(x, sau=0):
    try:
        s = _hien_tho(x, sau)
    except Exception:  # noqa: BLE001 — __repr__ của học viên hỏng thì vẫn không làm hỏng lượt chạy
        s = f"<{type(x).__name__}>"
    return s if len(s) <= DAI else s[: DAI - 1] + "…"


def _hien_tho(x, sau):
    if isinstance(x, (int, float, str, bool, type(None), range)):
        return repr(x)
    if sau >= 2:
        return "…" if isinstance(x, (list, tuple, dict, set)) else type(x).__name__
    if isinstance(x, list):
        return "[" + ", ".join(_hien(v, sau + 1) for v in x[:20]) + (", …" if len(x) > 20 else "") + "]"
    if isinstance(x, tuple):
        return "(" + ", ".join(_hien(v, sau + 1) for v in x[:20]) + ("," if len(x) == 1 else "") + ")"
    if isinstance(x, dict):
        cap = list(x.items())[:12]
        return "{" + ", ".join(f"{_hien(k, sau + 1)}: {_hien(v, sau + 1)}" for k, v in cap) + ("…" if len(x) > 12 else "") + "}"
    if isinstance(x, (set, frozenset)):
        return "{" + ", ".join(_hien(v, sau + 1) for v in list(x)[:20]) + "}" if x else "set()"
    t = type(x)
    if t.__repr__ is not object.__repr__:
        return repr(x)  # học viên tự viết __repr__ thì tôn trọng
    d = getattr(x, "__dict__", None)
    if isinstance(d, dict):
        # đối tượng tự viết không có __repr__ (nút danh sách liên kết, nút cây…): Ten(truong=giá trị)
        return t.__name__ + "(" + ", ".join(f"{k}={_hien(v, 2)}" for k, v in list(d.items())[:6]) + ")"
    return f"<{t.__name__}>"


def _bo_qua(ten, x):
    if ten.startswith("__") or ten == "viz":
        return True
    return isinstance(x, (types.ModuleType, types.FunctionType, types.BuiltinFunctionType, type, types.MethodType))


def _bien(frame):
    ra = []
    for k, v in list(frame.f_locals.items()):
        if not _bo_qua(k, v):
            ra.append([k, _hien(v)])
            if len(ra) >= TOI_DA_BIEN:
                break
    return ra


# ---------------------------------------------------------------- tính điều kiện


class _KhongTinh(Exception):
    """Biểu thức có phần không được tính lại (gọi code học viên…)."""


_CO_SAN = (int, float, str, bool, type(None), list, tuple, dict, set, frozenset, range)
_HAM_AN_TOAN = {"len", "abs", "min", "max", "sum", "range", "int", "float", "str", "bool", "sorted", "list",
                "tuple", "set", "round", "ord", "chr"}


def _an_toan(*gia_tri):
    for g in gia_tri:
        if not isinstance(g, _CO_SAN):
            raise _KhongTinh()
        if isinstance(g, (list, tuple, set, frozenset)) and not all(isinstance(v, _CO_SAN) for v in list(g)[:50]):
            raise _KhongTinh()


_SO_SANH = {
    ast.Eq: lambda a, b: a == b, ast.NotEq: lambda a, b: a != b, ast.Lt: lambda a, b: a < b,
    ast.LtE: lambda a, b: a <= b, ast.Gt: lambda a, b: a > b, ast.GtE: lambda a, b: a >= b,
    ast.In: lambda a, b: a in b, ast.NotIn: lambda a, b: a not in b,
    ast.Is: lambda a, b: a is b, ast.IsNot: lambda a, b: a is not b,
}
_TOAN = {
    ast.Add: lambda a, b: a + b, ast.Sub: lambda a, b: a - b, ast.Mult: lambda a, b: a * b,
    ast.Div: lambda a, b: a / b, ast.FloorDiv: lambda a, b: a // b, ast.Mod: lambda a, b: a % b,
    ast.Pow: lambda a, b: _mu(a, b),
}


def _mu(a, b):
    if abs(b) > 64:
        raise _KhongTinh()  # 10 ** 10**9 làm treo cả lượt chạy
    return a**b


class _Tinh:
    """Tính một biểu thức trên biến của khung đang chạy, trả (chữ đã thay giá trị, giá trị)."""

    def __init__(self, frame):
        self.loc, self.glo = frame.f_locals, frame.f_globals

    def ten(self, name):
        if name in self.loc:
            return self.loc[name]
        if name in self.glo:
            return self.glo[name]
        if hasattr(builtins, name):
            return getattr(builtins, name)
        raise NameError(name)

    def __call__(self, n):
        if isinstance(n, ast.Constant):
            return repr(n.value), n.value
        if isinstance(n, ast.Name):
            v = self.ten(n.id)
            s = _hien(v)
            return (s if len(s) <= 24 else n.id), v
        if isinstance(n, ast.Subscript):
            _, a = self(n.value)
            _, i = self(n.slice)
            _an_toan(a, i)
            v = a[i]
            return _hien(v), v
        if isinstance(n, ast.Attribute):
            _, o = self(n.value)
            d = getattr(o, "__dict__", None)
            if not isinstance(d, dict) or n.attr not in d:
                raise _KhongTinh()
            v = d[n.attr]
            return _hien(v), v
        if isinstance(n, ast.UnaryOp):
            s, a = self(n.operand)
            _an_toan(a)
            if isinstance(n.op, ast.Not):
                return f"not {s}", not a
            return (f"-{s}", -a) if isinstance(n.op, ast.USub) else (s, +a)
        if isinstance(n, ast.BinOp) and type(n.op) in _TOAN:
            sa, a = self(n.left)
            sb, b = self(n.right)
            _an_toan(a, b)
            return f"{sa} {_ky_hieu(n.op)} {sb}", _TOAN[type(n.op)](a, b)
        if isinstance(n, ast.Compare):
            sa, a = self(n.left)
            phan, kq = [sa], True
            for op, ve in zip(n.ops, n.comparators):
                sb, b = self(ve)
                _an_toan(a, b)
                phan += [_ky_hieu(op), sb]
                if kq:
                    kq = _SO_SANH[type(op)](a, b)
                a = b
            return " ".join(phan), kq
        if isinstance(n, ast.BoolOp):
            phan, kq = [], None
            la_and = isinstance(n.op, ast.And)
            for i, ve in enumerate(n.values):
                s, v = self(ve)
                phan.append(s)
                kq = v
                if (la_and and not v) or (not la_and and v):
                    phan += [ast.unparse(x) for x in n.values[i + 1:]]  # đoản mạch: vế sau không được tính
                    break
            return f" {'and' if la_and else 'or'} ".join(phan), kq
        if isinstance(n, ast.Call) and isinstance(n.func, ast.Name) and n.func.id in _HAM_AN_TOAN and not n.keywords:
            f = self.ten(n.func.id)
            if f is not getattr(builtins, n.func.id, None):
                raise _KhongTinh()  # học viên đặt lại tên len, max… thành hàm của mình
            doi = [self(a) for a in n.args]
            _an_toan(*[v for _, v in doi])
            v = f(*[v for _, v in doi])
            return _hien(v), v
        raise _KhongTinh()


def _ky_hieu(op):
    return {ast.Add: "+", ast.Sub: "-", ast.Mult: "*", ast.Div: "/", ast.FloorDiv: "//", ast.Mod: "%",
            ast.Pow: "**", ast.Eq: "==", ast.NotEq: "!=", ast.Lt: "<", ast.LtE: "<=", ast.Gt: ">", ast.GtE: ">=",
            ast.In: "in", ast.NotIn: "not in", ast.Is: "is", ast.IsNot: "is not"}[type(op)]


def _vang(v):
    return "True" if v else "False"


def dieu_kien(expr, frame, la_for=False):
    """Dữ liệu ô "Kiểm điều kiện" cho một dòng, hoặc None. Không bao giờ ném lỗi."""
    src = ast.unparse(expr)
    try:
        tinh = _Tinh(frame)
        if la_for:
            s, v = tinh(expr)
            return {"src": src, "thay": f"lặp trên {_hien(v)}", "kq": None, "for": True}
        ve = []
        if isinstance(expr, ast.BoolOp):
            la_and = isinstance(expr.op, ast.And)
            for x in expr.values:
                try:
                    _, v = tinh(x)
                    ve.append(_vang(v))
                except _KhongTinh:
                    ve.append("?")
                    break
                if (la_and and not v) or (not la_and and v):
                    ve += ["bỏ qua"] * (len(expr.values) - len(ve))
                    break
        thay, kq = tinh(expr)
        return {"src": src, "thay": thay, "ve": ve, "kq": _vang(kq)}
    except _KhongTinh:
        return {"src": src, "thay": None, "kq": None}
    except Exception as e:  # noqa: BLE001 — Python cũng sẽ gặp lỗi này khi chạy dòng đó
        return {"src": src, "thay": None, "kq": None, "loi": f"{type(e).__name__}: {e}"}


def _dong_dieu_kien(code):
    """Số dòng → (biểu thức, là vòng for) cho if/elif/while/for/assert."""
    try:
        cay = ast.parse(code)
    except SyntaxError:
        return {}
    ra = {}
    for n in ast.walk(cay):
        if isinstance(n, (ast.If, ast.While)):
            ra[n.lineno] = (n.test, False)
        elif isinstance(n, ast.Assert):
            ra[n.lineno] = (n.test, False)
        elif isinstance(n, ast.For):
            ra[n.lineno] = (n.iter, True)
    return ra


# ---------------------------------------------------------------- bộ ghi


class TungBuoc:
    def __init__(self, ten, code, tran):
        self.ten, self.tran, self.so = ten, tran, 0
        self.dk = _dong_dieu_kien(code)
        self.het = False
        recorder.current.register(ten, self)
        sys.settrace(self._toan_cuc)
        # code cấp module bắt đầu chạy sau khi bật: settrace có hiệu lực với khung mới, không cần gắn tay

    def trang_thai(self):
        return {"kieu": "tung-dong", "so": self.so}

    def ghi(self, su_kien):
        su_kien["v"] = self.ten
        try:
            recorder.current.emit(su_kien)
        except recorder.TraceLimit:
            self.go()
            raise

    def _toan_cuc(self, frame, event, arg):
        if self.het or frame.f_code.co_filename != FILE:
            return None
        return self._cuc_bo

    def _cuc_bo(self, frame, event, arg):
        if self.het:
            return None
        if recorder.current.dang_chup:
            return self._cuc_bo
        recorder.current.dang_chup = True  # repr của học viên có thể chạy code: không ghi chen
        try:
            if event == "line":
                self._dong(frame)
            elif event == "return" and frame.f_code.co_name != "<module>":
                self.ghi({"t": "buoc-ve", "f": frame.f_code.co_name, "gt": _hien(arg)})
        except recorder.TraceLimit:
            raise
        except Exception:  # noqa: BLE001 — lỗi của bộ ghi không được thành lỗi của bài làm
            self.go()
        finally:
            recorder.current.dang_chup = False
        return None if self.het else self._cuc_bo

    def _dong(self, frame):
        if self.so >= self.tran:
            self.ghi({"t": "buoc-het", "tran": self.tran})
            self.go()
            return
        self.so += 1
        n = frame.f_lineno
        su_kien = {
            "t": "buoc",
            "n": n,
            "f": "" if frame.f_code.co_name == "<module>" else frame.f_code.co_name,
            "ngan": _ngan(frame),
            "bien": _bien(frame),
            "o": _da_in(),
        }
        if n in self.dk:
            expr, la_for = self.dk[n]
            su_kien["dk"] = dieu_kien(expr, frame, la_for)
        self.ghi(su_kien)

    def go(self):
        """Thôi ghi: gỡ móc toàn cục và móc của mọi khung đang chạy, chương trình chạy tiếp hết tốc độ."""
        self.het = True
        sys.settrace(None)
        f = sys._getframe()
        while f is not None:
            if f.f_code.co_filename == FILE:
                f.f_trace = None
            f = f.f_back


def _ngan(frame):
    ra = []
    f = frame
    while f is not None and len(ra) < SAU_NGAN:
        if f.f_code.co_filename == FILE:
            if f.f_code.co_name == "<module>":
                ra.append("chương trình")
            else:
                co = f.f_code
                ten_ts = co.co_varnames[: co.co_argcount]
                ts = ", ".join(f"{t}={_hien(f.f_locals.get(t), 2)}" for t in ten_ts if t != "self")
                ra.append(f"{co.co_name}({ts})")
        f = f.f_back
    return ra[::-1]


def _da_in():
    try:
        return sys.stdout.tell()
    except Exception:  # noqa: BLE001
        return 0
