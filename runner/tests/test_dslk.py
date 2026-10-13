import io
import json

import pytest

from viz import recorder, theo_doi
from viz.dslk import DSLK


def ghi():
    buf = io.StringIO()
    recorder.current = recorder.Recorder(out=buf)
    return buf


def dong(buf):
    return [json.loads(x) for x in buf.getvalue().splitlines() if '"snap"' not in x]


@pytest.fixture(autouse=True)
def tat_ghi_sau_moi_test():
    yield
    theo_doi.tat()
    recorder.current = recorder.Recorder(out=None, enabled=False)


def test_tao_tu_danh_sach_va_noi_cat():
    buf = ghi()
    ds = DSLK.tu([1, 2], ten="ds")
    ds.dau.tiep = None
    ev = dong(buf)
    assert [e["t"] for e in ev] == ["node", "link", "node", "link", "link"]
    assert ev[1] == {"t": "link", "id": None, "w": 0, "v": "ds"}
    assert ev[3] == {"t": "link", "id": 0, "w": 1, "v": "ds"}
    assert ev[4] == {"t": "link", "id": 0, "w": None, "v": "ds"}
    assert list(ds) == [1]


def test_doc_ghi_gia_tri_ten_tieng_anh():
    buf = ghi()
    ds = DSLK(ten="L")
    ds.head = ds.Node(5)
    assert ds.head.val == 5
    ds.head.data = 6
    assert [e["t"] for e in dong(buf)][-2:] == ["read", "set"]
    assert ds.trang_thai() == {"kieu": "dslk", "dau": 0, "nut": [[0, 6, None, None]], "tro": []}


DAO_NGUOC = """
import viz
ds = viz.DSLK.tu([1, 2, 3], ten="ds")
prev = None
cur = ds.dau
while cur is not None:
    nxt = cur.tiep
    cur.tiep = prev
    prev = cur
    cur = nxt
ds.dau = prev
"""


def test_bien_tro_vao_nut_tu_hien_thanh_ptr():
    buf = ghi()
    exec(compile(DAO_NGUOC, theo_doi.FILE, "exec"), {"__name__": "__main__"})
    ev = dong(buf)
    ptr = [e["p"] for e in ev if e["t"] == "ptr"]
    assert ptr[0] == [["cur", 0]]
    assert ["nxt", 1] in ptr[1] and ["cur", 0] in ptr[1]
    assert ptr[-1] == [["prev", 2]]  # vòng cuối: cur = None, chỉ còn prev
    assert [e for e in ev if e["t"] == "link"][-1] == {"t": "link", "id": None, "w": 2, "v": "ds"}


def test_khong_ghi_trace_thi_khong_theo_doi():
    recorder.current = recorder.Recorder(out=None, enabled=False)
    exec(compile(DAO_NGUOC, theo_doi.FILE, "exec"), {"__name__": "__main__"})
    import sys
    assert sys.gettrace() is None


def test_khong_noi_duoc_nut_danh_sach_khac():
    ghi()
    a, b = DSLK(ten="a"), DSLK(ten="b")
    with pytest.raises(TypeError):
        a.dau = b.Nut(1)


def test_chup_trang_thai_khong_chen_su_kien():
    """trang_thai() chạy code bài làm (ở đây là lambda) thì bộ theo dõi không được ghi ptr chen vào giữa."""
    buf = ghi()
    code = DAO_NGUOC + "import viz.recorder as _r\n_r.current.register('x', type('X', (), {'trang_thai': lambda s: 0})())\n"
    exec(compile(code, theo_doi.FILE, "exec"), {"__name__": "__main__"})
    ev = [json.loads(x) for x in buf.getvalue().splitlines()]
    cuoi = [e for e in ev if e["t"] == "snap"][-1]["state"]["ds"]["tro"]
    ptr = [e["p"] for e in ev if e["t"] == "ptr"]
    assert ptr[-1] == cuoi == [["prev", 2]]
