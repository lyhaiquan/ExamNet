import io
import json

import pytest

from viz import recorder
from viz.bang import Bang


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


def test_gan_roi_doc_sinh_cell_w_cell_r():
    buf = ghi()
    dp = Bang(2, 3, ten="dp")
    dp[1][2] = 5
    assert dp[1][2] == 5
    e = dong(buf)
    assert e[0]["state"] == {"dp": {"kieu": "luoi", "o": [[0, 0, 0], [0, 0, 0]]}}
    assert e[1] == {"t": "cell_w", "v": "dp", "i": 1, "j": 2, "x": 5}
    assert e[2] == {"t": "cell_r", "v": "dp", "i": 1, "j": 2}


def test_cong_don_sinh_ca_doc_lan_ghi():
    buf = ghi()
    dp = Bang(1, 1, ten="dp")
    dp[0][0] += 3
    assert [x["t"] for x in dong(buf)] == ["snap", "cell_r", "cell_w"]


def test_chi_so_am_quy_ve_duong_va_chi_so_sai_bao_loi():
    buf = ghi()
    dp = Bang(2, 2, ten="dp")
    dp[-1][-1] = 7
    assert dong(buf)[-1] == {"t": "cell_w", "v": "dp", "i": 1, "j": 1, "x": 7}
    with pytest.raises(IndexError):
        dp[2][0]
    with pytest.raises(IndexError):
        dp[0][2] = 1


def test_gan_ca_hang_va_lat_cat_ghi_tung_o():
    buf = ghi()
    dp = Bang(2, 3, ten="dp")
    dp[0] = [1, 2, 3]
    dp[1][1:] = [8, 9]
    ghi_o = [(x["i"], x["j"], x["x"]) for x in dong(buf) if x["t"] == "cell_w"]
    assert ghi_o == [(0, 0, 1), (0, 1, 2), (0, 2, 3), (1, 1, 8), (1, 2, 9)]
    assert dp.trang_thai()["o"] == [[1, 2, 3], [0, 8, 9]]


def test_in_bang_khong_sinh_su_kien():
    buf = ghi()
    dp = Bang(3, 3, ten="dp")
    print(dp, dp[0], len(dp), len(dp[0]), dp.so_hang, dp.so_cot)
    assert [x["t"] for x in dong(buf)] == ["snap"]


def test_nhan_hai_truc_nam_trong_trang_thai():
    buf = ghi()
    Bang(3, 4, ten="dp", nhan_hang=" AB", nhan_cot=" XYZ")
    s = dong(buf)[0]["state"]["dp"]
    assert s["hang"] == [" ", "A", "B"]
    assert s["cot"] == [" ", "X", "Y", "Z"]


def test_bang_tu_luoi_co_san_va_danh_dau():
    buf = ghi()
    g = Bang.tu(["#.", ".#"], ten="g")
    assert g[0][1] == "."
    g.danh_dau(0, 1)
    e = dong(buf)
    assert e[0]["state"]["g"]["o"] == [["#", "."], [".", "#"]]
    assert e[-1] == {"t": "mark", "v": "g", "i": 0, "j": 1}
    with pytest.raises(ValueError):
        Bang.tu(["##", "#"])


def test_dp_i_j_dang_tuple():
    buf = ghi()
    dp = Bang(2, 2, ten="dp")
    dp[1, 0] = 4
    assert dp[1, 0] == 4
    assert [x["t"] for x in dong(buf)] == ["snap", "cell_w", "cell_r"]


def test_keyframe_giu_o_da_ghi_va_o_danh_dau():
    buf = ghi()
    dp = Bang(2, 2, ten="dp")
    dp[0][1] = 3
    dp.danh_dau(1, 1)
    for _ in range(200):
        dp[0][0]
    snaps = [x for x in dong(buf) if x["t"] == "snap"]
    s = snaps[-1]["state"]["dp"]
    assert s["ghi"] == [[0, 1]]
    assert s["dau"] == [[1, 1]]

