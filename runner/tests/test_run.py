import json
import pathlib
import subprocess
import sys

RUN = pathlib.Path(__file__).resolve().parent.parent / "run.py"

NOI_BOT = """\
import viz

def sap_xep(a):
    n = len(a)
    for i in range(n - 1):
        for j in range(n - 1 - i):
            if a[j] > a[j + 1]:
                a[j], a[j + 1] = a[j + 1], a[j]
        viz.key("Hết lượt " + str(i + 1))

a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
"""


def chay(job):
    p = subprocess.run(
        [sys.executable, str(RUN)],
        input=json.dumps(job).encode("utf-8"),
        capture_output=True,
        timeout=20,
    )
    lines = [json.loads(x) for x in p.stdout.decode("utf-8").splitlines() if x.strip()]
    return lines[:-1], lines[-1]


def test_noi_bot_ok_co_trace():
    trace, kq = chay({"code": NOI_BOT, "input": "5 1 4 2\n", "trace": True, "views": [{"v": "a", "kieu": "mang"}]})
    assert kq["t"] == "result"
    assert kq["ok"] is True
    assert kq["stdout"] == "1 2 4 5\n"
    loai = [e["t"] for e in trace]
    assert loai[0] == "hdr" and loai[-1] == "end"
    assert "set" in loai and "key" in loai
    assert trace[0]["views"] == [{"v": "a", "kieu": "mang"}]


def test_khong_trace_thi_chi_co_ket_qua():
    trace, kq = chay({"code": NOI_BOT, "input": "3 2 1\n", "trace": False})
    assert trace == []
    assert kq["stdout"] == "1 2 3\n"


def test_loi_runtime_bao_dung_dong():
    trace, kq = chay({"code": "x = 1\nraise ValueError('sai rồi')\n", "input": "", "trace": False})
    assert kq["ok"] is False
    assert "ValueError" in kq["error"] and "bai_lam.py" in kq["error"]
    assert "dòng 2" in kq["error"]
    assert "run.py" not in kq["error"]


def test_loi_cu_phap():
    _, kq = chay({"code": "def f(:\n  pass\n", "trace": False})
    assert kq["ok"] is False and "cú pháp" in kq["error"]


def test_vong_lap_vo_han_doc_mang_bi_cat_trace():
    code = "import viz\na = viz.Mang([1, 2])\nwhile True:\n    a[0]\n"
    trace, kq = chay({"code": code, "trace": True, "max_events": 500})
    assert kq["truncated"] is True and kq["ok"] is True
    assert trace[-1] == {"t": "end", "truncated": True}


def test_input_tieng_viet():
    _, kq = chay({"code": "print(input()[::-1])", "input": "Lê Hải\n", "trace": False})
    assert kq["stdout"] == "iảH êL\n"


def test_sys_exit_0_van_ok():
    _, kq = chay({"code": "import sys\nprint(1)\nsys.exit(0)\n", "trace": False})
    assert kq["ok"] is True and kq["stdout"] == "1\n"


def test_vo_cuc_ghi_thanh_chu_de_java_doc_duoc():
    # json.dumps mặc định ghi Infinity/NaN: không phải JSON chuẩn, Jackson từ chối cả trace
    code = "import viz\na = viz.Mang([float('inf'), 1])\na[1] = -float('inf')\nb = viz.Bang(1, 2, float('inf'))\nb[0][0] = 3\nprint(1)"
    trace, kq = chay({"code": code, "input": "", "trace": True})
    assert kq["ok"] is True
    raw = json.dumps(trace, ensure_ascii=False, allow_nan=False)  # không còn số vô cực nào
    assert '"∞"' in raw and '"-∞"' in raw
    assert trace[1]["state"]["a"] == ["∞", 1]
