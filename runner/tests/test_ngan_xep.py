import io
import json

import pytest

from viz import recorder
from viz.ngan_xep import HangDoi, NganXep


def ghi(**kw):
    buf = io.StringIO()
    recorder.current = recorder.Recorder(out=buf, **kw)
    return buf


def dong(buf):
    return [json.loads(x) for x in buf.getvalue().splitlines()]


@pytest.fixture(autouse=True)
def tat_ghi_sau_moi_test():
    yield
    recorder.current = recorder.Recorder(out=None, enabled=False)


def test_push_push_pop_sinh_dung_su_kien_va_lay_ra_phan_tu_cuoi():
    buf = ghi()
    st = NganXep(ten="st")
    st.push(1)
    st.push(2)
    assert st.pop() == 2
    e = dong(buf)
    assert [x["t"] for x in e] == ["snap", "push", "push", "pop"]
    assert e[0]["state"] == {"st": {"kieu": "ngan-xep", "ds": []}}
    assert e[3] == {"t": "pop", "v": "st", "x": 2}
    assert list(st) == [1]


def test_ten_quen_cua_list_cung_sinh_su_kien():
    buf = ghi()
    st = NganXep(ten="st")
    st.append("(")
    assert st[-1] == "("
    assert st.top() == "("
    e = dong(buf)
    assert [x["t"] for x in e] == ["snap", "push", "read", "read"]
    assert e[2] == {"t": "read", "v": "st", "i": 0}


def test_kiem_tra_rong_va_duyet_khong_sinh_su_kien():
    buf = ghi()
    st = NganXep([1, 2, 3], ten="st")
    assert len(st) == 3 and st and not st.rong()
    assert list(st) == [1, 2, 3]
    print(st)
    assert [x["t"] for x in dong(buf)] == ["snap"]


def test_pop_rong_bao_loi_ro_rang():
    ghi()
    st = NganXep(ten="st")
    with pytest.raises(IndexError, match="rỗng"):
        st.pop()


def test_khong_doi_duoc_du_lieu_lang_le_bang_ham_cua_list():
    ghi()
    st = NganXep([3, 1, 2], ten="st")
    for ham in ("sort", "reverse", "insert", "extend", "clear", "remove"):
        assert not hasattr(st, ham), ham


def test_hang_doi_vao_cuoi_ra_dau():
    buf = ghi()
    q = HangDoi(ten="q")
    q.enq("1")
    q.append("10")
    assert q[0] == "1"
    assert q.popleft() == "1"
    assert q.deq() == "10"
    e = dong(buf)
    assert [x["t"] for x in e] == ["snap", "enq", "enq", "read", "deq", "deq"]
    assert e[0]["state"] == {"q": {"kieu": "hang-doi", "ds": []}}
    assert e[4] == {"t": "deq", "v": "q", "x": "1"}
    with pytest.raises(IndexError):
        q.deq()


def test_gia_tri_la_ghi_thanh_chu_khong_lam_hong_bai_chay():
    buf = ghi()
    st = NganXep(ten="st")
    st.push({1, 2})  # set không ghi được JSON
    st.push((2, 3))  # tuple thành mảng JSON
    e = dong(buf)
    assert e[1]["x"] == str({1, 2})
    assert e[2]["x"] == [2, 3]


def test_keyframe_chup_ca_ngan_xep_lan_hang_doi():
    buf = ghi()
    st = NganXep(ten="st")
    q = HangDoi(ten="q")
    for i in range(100):
        st.push(i)
        q.enq(i)
    snaps = [x for x in dong(buf) if x["t"] == "snap"]
    cuoi = snaps[-1]["state"]
    assert cuoi["st"]["ds"] == list(range(100))
    assert cuoi["q"]["ds"] == list(range(100))
