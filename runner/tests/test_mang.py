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


def test_moi_ham_doi_du_lieu_deu_ghi_lai():
    """Lỗi cũ: append/sort/reverse đổi mảng mà không ghi gì, hình vẽ lệch với dữ liệu thật."""
    buf = ghi()
    a = Mang([3, 1], ten="a")
    a.append(2)
    a.insert(0, 9)
    x = a.pop()
    a.sort()
    a.reverse()
    a.extend([7])
    a.remove(7)
    del a[0]
    a[0:1] = [5, 6]
    a += [4]
    a.pop(0)
    a.clear()
    ev = [e for e in dong(buf) if e["t"] != "snap"]
    assert x == 2
    assert [e["t"] for e in ev] == [
        "push", "ins", "pop", "all", "all", "all", "all", "all", "all", "all", "pop", "all"]
    assert ev[0] == {"t": "push", "x": 2, "v": "a"}
    assert ev[1] == {"t": "ins", "i": 0, "x": 9, "v": "a"}
    assert ev[2] == {"t": "pop", "i": 3, "x": 2, "v": "a"}
    assert ev[3]["ds"] == [1, 3, 9]


def test_phat_lai_su_kien_ra_dung_mang_cuoi():
    """Áp lần lượt mọi sự kiện lên một list rỗng ruột phải ra đúng mảng thật — như bộ vẽ làm."""
    buf = ghi()
    a = Mang([4, 2, 8], ten="a")
    a.append(1)
    a.insert(-1, 6)
    a.pop(1)
    a[0] = 5
    a.sort(reverse=True)
    a.insert(100, 0)
    b = None
    for e in dong(buf):
        t = e["t"]
        if t == "snap":
            b = list(e["state"]["a"])
        elif t == "set":
            b[e["i"]] = e["x"]
        elif t == "push":
            b.append(e["x"])
        elif t == "pop":
            b.pop(e["i"])
        elif t == "ins":
            b.insert(e["i"], e["x"])
        elif t == "all":
            b = list(e["ds"])
    assert b == list(a)
