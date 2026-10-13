import io
import json

import pytest

from viz import recorder
from viz.cay import Cay


def ghi():
    buf = io.StringIO()
    recorder.current = recorder.Recorder(out=buf)
    return buf


def dong(buf):
    return [json.loads(x) for x in buf.getvalue().splitlines()]


@pytest.fixture(autouse=True)
def tat_ghi_sau_moi_test():
    yield
    recorder.current = recorder.Recorder(out=None, enabled=False)


def chen(T, nut, k):
    if nut is None:
        return T.Nut(k)
    if k < nut.khoa:
        nut.trai = chen(T, nut.trai, k)
    else:
        nut.phai = chen(T, nut.phai, k)
    return nut


def test_chen_bst_sinh_node_edge_read():
    buf = ghi()
    T = Cay(ten="T")
    for k in [5, 3, 8]:
        T.goc = chen(T, T.goc, k)
    ev = [e for e in dong(buf) if e["t"] != "snap"]
    assert ev[0] == {"t": "node", "id": 0, "x": 5, "v": "T"}
    assert ev[1] == {"t": "edge", "u": None, "c": None, "w": 0, "v": "T"}
    # chèn 3: đọc khoá gốc, tạo nút, gắn làm con trái; gán lại gốc cũ thì không ghi
    assert [e["t"] for e in ev[2:5]] == ["read", "node", "edge"]
    assert ev[4] == {"t": "edge", "u": 0, "c": "L", "w": 1, "v": "T"}
    assert T.trang_thai() == {
        "kieu": "cay", "goc": 0,
        "nut": [[0, 5, 1, 2, None, True], [1, 3, None, None, None, True], [2, 8, None, None, None, True]]}


def test_ten_tieng_anh_va_danh_dau():
    buf = ghi()
    T = Cay()
    T.root = T.Node(1)
    T.root.right = T.Node(2)
    assert T.root.key == 1 and T.goc.phai.khoa == 2
    T.danh_dau(T.root.right, "xong")
    assert dong(buf)[-1] == {"t": "mark", "id": 1, "m": "xong", "v": "T"}
    with pytest.raises(ValueError):
        T.danh_dau(T.root, "tim")


def test_khong_gan_duoc_nut_cay_khac():
    ghi()
    A, B = Cay(ten="A"), Cay(ten="B")
    with pytest.raises(TypeError):
        A.goc = B.Nut(1)
    with pytest.raises(TypeError):
        A.goc = 5


def test_thuoc_tinh_rieng_cua_nut_khong_ghi():
    buf = ghi()
    T = Cay()
    n = T.Nut(4)
    n.cao = 1
    assert n.cao == 1
    assert [e["t"] for e in dong(buf) if e["t"] != "snap"] == ["node"]
