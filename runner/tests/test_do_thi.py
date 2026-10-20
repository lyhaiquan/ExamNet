import io
import json

import pytest

from viz import recorder
from viz.do_thi import DoThi


def ghi():
    buf = io.StringIO()
    recorder.current = recorder.Recorder(out=buf)
    return buf


def dong(buf):
    return [json.loads(x) for x in buf.getvalue().splitlines() if '"snap"' not in x]


@pytest.fixture(autouse=True)
def tat_ghi_sau_moi_test():
    yield
    recorder.current = recorder.Recorder(out=None, enabled=False)


def test_them_canh_va_duyet_ke_ghi_tung_canh():
    buf = ghi()
    g = DoThi(3, ten="g", bat_dau=1)
    g.them_canh(1, 2)
    g.them_canh(1, 3)
    assert list(g.ke(1)) == [2, 3]
    assert list(g.ke(3)) == [1]  # vô hướng: cạnh có cả hai chiều
    ev = dong(buf)
    assert ev[0] == {"t": "edge", "a": 1, "b": 2, "x": None, "v": "g"}
    assert [e["t"] for e in ev[2:]] == ["read", "read", "read", "read", "read"]
    assert ev[2] == {"t": "read", "id": 1, "v": "g"}
    assert ev[3] == {"t": "read", "a": 1, "b": 2, "v": "g"}


def test_co_huong_co_trong_so():
    ghi()
    g = DoThi(2, co_huong=True, co_trong_so=True)
    g.them_canh(0, 1, 7)
    assert list(g.ke(0)) == [(1, 7)]
    assert list(g.ke(1)) == []
    with pytest.raises(ValueError):
        g.them_canh(0, 1)


def test_danh_dau_nhan_va_anh_chup():
    buf = ghi()
    g = DoThi(3, ten="g")
    g.them_canh(0, 1)
    g.danh_dau(0, "xong")
    g.danh_dau_canh(1, 0, "xong")
    g.nhan(1, 1)
    g.vi_tri(2, 0.5, 0.25)
    assert dong(buf)[-1] == {"t": "pos", "id": 2, "x": 0.5, "y": 0.25, "v": "g"}
    assert g.trang_thai() == {
        "kieu": "do-thi", "co_huong": False, "dinh": [0, 1, 2], "canh": [[0, 1, None]],
        "mau": [[0, "xong"]], "mau_canh": [[0, 1, "xong"]], "nhan": [[1, "1"]], "vt": [[2, 0.5, 0.25]]}


def test_dinh_hay_canh_khong_co_thi_bao_loi():
    ghi()
    g = DoThi(2)
    with pytest.raises(IndexError):
        g.them_canh(0, 5)
    with pytest.raises(ValueError):
        g.danh_dau_canh(0, 1)
    with pytest.raises(ValueError):
        g.danh_dau(0, "tim")


def test_to_canh_dinh_bac_lon_khong_do_danh_sach_ke():
    import time

    g = DoThi(30_001, ten="g", bat_dau=1)
    for v in range(2, 30_002):
        g.them_canh(1, v)
    t = time.time()
    for v in range(2, 30_002):
        g.danh_dau_canh(v, 1, "xong")
    assert time.time() - t < 1, "tô cạnh phải O(1), không dò hết 30 000 đỉnh kề của đỉnh 1"
