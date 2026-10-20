"""Chạy từng bước (bộ vẽ tung-dong) cho bài luyện tập: dòng, biến, điều kiện, lỗi có gợi ý."""

import io
import json

from run import chay

VIEWS = [{"v": "buoc", "kieu": "tung-dong"}]

CHEN = """n = int(input())
a = list(map(int, input().split()))
for i in range(1, n):
    key = a[i]
    j = i - 1
    while j >= 0 and a[j] > key:
        a[j + 1] = a[j]
        j -= 1
    a[j + 1] = key
print(*a)
"""


def chay_buoc(code, vao="", **them):
    out = io.StringIO()
    chay({"code": code, "input": vao, "trace": True, "views": VIEWS, **them}, out)
    su_kien = [json.loads(d) for d in out.getvalue().splitlines()]
    return su_kien[:-1], su_kien[-1]


def buoc(trace):
    return [e for e in trace if e["t"] == "buoc"]


def test_ghi_tung_dong_va_bien_truoc_khi_chay():
    trace, kq = chay_buoc(CHEN, "3\n3 1 2\n")
    assert kq["ok"] and kq["stdout"] == "1 2 3\n"
    b = buoc(trace)
    assert [x["n"] for x in b[:5]] == [1, 2, 3, 4, 5]
    assert b[0]["bien"] == []  # dòng 1 chưa chạy: chưa có n
    assert ["n", "3"] in b[1]["bien"]
    assert trace[-2] == {"t": "buoc-xong", "out": "1 2 3\n", "ok": True, "v": "buoc"}


def test_dieu_kien_doan_mach_nhu_python():
    trace, _ = chay_buoc(CHEN, "3\n3 1 2\n")
    dk = [x["dk"] for x in buoc(trace) if x["n"] == 6]
    assert dk[0] == {"src": "j >= 0 and a[j] > key", "thay": "0 >= 0 and 3 > 1", "ve": ["True", "True"],
                     "kq": "True"}
    # j = -1: vế sau KHÔNG được tính (a[-1] hợp lệ trong Python nhưng sai ý nghĩa)
    het = [d for d in dk if d["ve"][:1] == ["False"]]
    assert het and het[0]["thay"] == "-1 >= 0 and a[j] > key" and het[0]["ve"] == ["False", "bỏ qua"]
    assert het[0]["kq"] == "False"


def test_dieu_kien_goi_ham_tu_viet_khong_tinh_lai():
    code = "dem = 0\ndef f(x):\n    global dem\n    dem += 1\n    return x > 0\nif f(1):\n    print(dem)\n"
    trace, kq = chay_buoc(code)
    assert kq["stdout"] == "1\n"  # f chỉ chạy đúng một lần
    dk = [x["dk"] for x in buoc(trace) if x["n"] == 6][0]
    assert dk["kq"] is None and dk["src"] == "f(1)"


def test_de_quy_co_ngan_goi_va_gia_tri_tra_ve():
    code = "def fib(n):\n    if n < 2:\n        return n\n    return fib(n - 1) + fib(n - 2)\nprint(fib(3))\n"
    trace, kq = chay_buoc(code)
    assert kq["stdout"] == "2\n"
    sau = max(len(x["ngan"]) for x in buoc(trace))
    assert sau == 4 and any(x["ngan"][-1] == "fib(n=1)" for x in buoc(trace))
    assert {"t": "buoc-ve", "f": "fib", "gt": "2", "v": "buoc"} in trace


def test_loi_chi_so_co_dong_va_goi_y():
    code = "a = [1, 2, 3]\nfor i in range(4):\n    print(a[i])\n"
    trace, kq = chay_buoc(code)
    assert not kq["ok"] and kq["error_line"] == 3 and kq["error_kind"] == "IndexError"
    assert "Gợi ý: Chỉ số vượt ngoài mảng" in kq["error"]
    loi = [e for e in trace if e["t"] == "loi"][0]
    assert loi["n"] == 3 and loi["kieu"] == "IndexError"
    assert trace[-2]["out"] == "1\n2\n3\n"


def test_goi_sai_phuong_thuc_cua_lop():
    _, kq = chay_buoc("st = []\nst.push(1)\n")
    assert kq["error_line"] == 2 and kq["error_kind"] == "AttributeError"
    assert "Kiểu list không có 'push'. Trong Python dùng: append." in kq["error"]
    _, kq = chay_buoc("s = 'abc'\nprint(s.uper())\n")
    assert "Có phải ý em là 'upper'?" in kq["error"]


def test_loi_cu_phap_bao_dong_khong_chay_buoc_nao():
    trace, kq = chay_buoc("n = 3\nif n > 2\n    print(n)\n")
    assert not kq["ok"] and kq["error_line"] == 2 and kq["error_kind"] == "SyntaxError"
    assert "Thiếu dấu hai chấm" in kq["error"]
    assert buoc(trace) == [] and [e for e in trace if e["t"] == "loi"][0]["n"] == 2


def test_qua_tran_thi_thoi_ghi_nhung_van_chay_het():
    code = "s = 0\nfor i in range(100000):\n    s += i\nprint(s)\n"
    trace, kq = chay_buoc(code, max_steps=50)
    assert kq["ok"] and kq["stdout"] == "4999950000\n" and not kq["truncated"]
    assert len(buoc(trace)) == 50 and {"t": "buoc-het", "tran": 50, "v": "buoc"} in trace


def test_doi_tuong_tu_viet_hien_theo_truong():
    code = "class Nut:\n    def __init__(self, x):\n        self.x = x\n        self.tiep = None\nn = Nut(5)\nprint(n.x)\n"
    trace, _ = chay_buoc(code)
    cuoi = buoc(trace)[-1]["bien"]
    assert ["n", "Nut(x=5, tiep=None)"] in cuoi


def test_khong_bat_thi_khong_co_buoc():
    out = io.StringIO()
    chay({"code": CHEN, "input": "3\n3 1 2\n", "trace": True, "views": []}, out)
    assert '"buoc"' not in out.getvalue()
