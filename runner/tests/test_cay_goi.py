import io
import json

import pytest

import run
from viz import cay_goi, recorder, theo_doi

FIB = """
import viz
nho = {}


def fib(n):
    if n in nho:
        viz.cat("đã nhớ")
        return nho[n]
    kq = n if n < 2 else fib(n - 1) + fib(n - 2)
    nho[n] = kq
    return kq


print(fib(int(input())))
"""


def chay(code, du_lieu, views):
    buf = io.StringIO()
    run.chay({"code": code, "input": du_lieu, "trace": True, "views": views}, buf)
    return [json.loads(x) for x in buf.getvalue().splitlines()]


@pytest.fixture(autouse=True)
def don_dep():
    yield
    theo_doi.tat()
    cay_goi.tat()
    recorder.current = recorder.Recorder(out=None, enabled=False)


def test_cay_loi_goi_fib_co_nho():
    ev = chay(FIB, "4\n", [{"v": "fib", "kieu": "cay-goi"}])
    assert ev[-1]["stdout"] == "3\n"
    goi = [e for e in ev if e["t"] == "call"]
    assert [g["a"] for g in goi] == ["fib(4)", "fib(3)", "fib(2)", "fib(1)", "fib(0)", "fib(1)", "fib(2)"]
    assert goi[0]["p"] is None and goi[1]["p"] == 0 and goi[5]["p"] == 1
    assert [e["id"] for e in ev if e["t"] == "mark"] == [5, 6]  # fib(1), fib(2) lần hai lấy từ bảng nhớ
    ret = {e["id"]: e["x"] for e in ev if e["t"] == "ret"}
    assert ret[0] == "3" and ret[6] == "1"
    dau = [e for e in ev if e["t"] == "snap"][0]
    assert dau["state"]["fib"] == {"kieu": "cay-goi", "nut": [], "ngan": []}


def test_khong_ghi_trace_thi_khong_theo_doi():
    buf = io.StringIO()
    run.chay({"code": FIB, "input": "30\n", "trace": False, "views": [{"v": "fib", "kieu": "cay-goi"}]}, buf)
    kq = json.loads(buf.getvalue().splitlines()[-1])
    assert kq["stdout"] == "832040\n"


def test_qua_toi_da_thi_dung_ve_va_bao():
    ev = chay("def f(n):\n    return 0 if n == 0 else f(n - 1) + f(n - 1)\nf(9)\n", "", [{"v": "f", "kieu": "cay-goi"}])
    assert len([e for e in ev if e["t"] == "call"]) == cay_goi.TOI_DA
    assert any(e["t"] == "say" and "quá lớn" in e["s"] for e in ev)


def test_chay_tung_dong_ghi_dong_va_bien():
    code = "a = 1\nb = a + 1\nprint(a + b)\n"
    ev = chay(code, "", [{"v": "code", "kieu": "code-bien"}])
    assert [e["n"] for e in ev if e["t"] == "line"] == [1, 2, 3]
    bien = [e["d"] for e in ev if e["t"] == "vars"]
    assert bien[-1] == [["a", "1"], ["b", "2"]]
    assert ev[1]["state"]["code"]["src"] == code
