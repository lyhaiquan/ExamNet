import io
import json

import pytest

from viz import recorder
from viz.mang import Mang


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


def test_doi_cho_sinh_hai_lenh_set():
    buf = ghi()
    a = Mang([5, 1], ten="a")
    a[0], a[1] = a[1], a[0]
    t = [e["t"] for e in dong(buf)]
    assert t == ["snap", "read", "read", "set", "set"]
    assert list(a) == [1, 5]


def test_snap_dau_tien_chup_trang_thai():
    buf = ghi()
    Mang([3, 2, 1], ten="b")
    assert dong(buf)[0] == {"t": "snap", "n": 0, "state": {"b": [3, 2, 1]}}


def test_chi_so_am_quy_ve_duong():
    buf = ghi()
    a = Mang([1, 2, 3])
    a[-1]
    assert dong(buf)[-1] == {"t": "read", "v": "a", "i": 2}


def test_keyframe_moi_200_su_kien():
    buf = ghi()
    a = Mang([1, 2])
    for _ in range(200):
        a[0]
    snaps = [e for e in dong(buf) if e["t"] == "snap"]
    assert len(snaps) == 2 and snaps[1]["n"] == 200


def test_vuot_tran_thi_dung():
    ghi(max_events=3)
    a = Mang([1, 2, 3], ten="a")
    with pytest.raises(recorder.TraceLimit):
        for _ in range(10):
            a[0]


def test_tat_ghi_thi_khong_sinh_su_kien():
    recorder.current = recorder.Recorder(out=None, enabled=False)
    a = Mang([3, 2], ten="a")
    a[0] = 9
    assert list(a) == [9, 2]


def test_say_key_con_tro():
    buf = ghi()
    a = Mang([1])
    recorder.say("đổi chỗ")
    recorder.key("hết lượt")
    a.con_tro("i", 0)
    assert [e["t"] for e in dong(buf)] == ["snap", "say", "key", "ptr"]
