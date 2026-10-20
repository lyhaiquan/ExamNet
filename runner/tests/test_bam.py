import io
import json

import pytest

import viz
from viz import recorder
from viz.bam import BangBam


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


def test_day_chuyen_them_tim_xoa():
    buf = ghi()
    T = BangBam(3, ten="T")
    T[1].append(4)
    T[1].append(7)
    assert 7 in T[1]
    assert 9 not in T[0]
    T[1].remove(4)
    ev = dong(buf)
    assert ev[0] == {"t": "bucket", "i": 1, "op": "add", "x": 4, "k": 0, "v": "T"}
    assert [(e["op"], e.get("k")) for e in ev[2:]] == [("read", 0), ("read", 1), ("read", 0), ("del", 0)]
    assert T.trang_thai() == {"kieu": "bam", "mo": False, "o": [[], [7], []]}
    with pytest.raises(TypeError):
        T[0] = 5


def test_dia_chi_mo_doc_ghi_va_dau_xoa():
    buf = ghi()
    T = BangBam(4, mo=True, ten="T")
    T[2] = 6
    assert T[2] == 6 and T[3] is None
    T[2] = viz.XOA
    ev = dong(buf)
    assert [e["t"] for e in ev] == ["set", "read", "read", "set"]
    assert ev[-1]["x"] == "XOA"
    assert T.trang_thai()["o"][2] is viz.XOA
    assert json.dumps(T.trang_thai(), default=str) == '{"kieu": "bam", "mo": true, "o": [null, null, "XOA", null]}'
