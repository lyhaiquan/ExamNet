"""Bộ sinh test, lời giải mẫu và luật soi trace của bài nổi bọt phải khớp nhau: lời giải mẫu qua luật của chính
nó, nếu không mọi học viên làm đúng đều bị chấm TRACE_RULE."""

import json
import pathlib
import subprocess
import sys

RUNNER = pathlib.Path(__file__).resolve().parent.parent
BAI = RUNNER.parent / "content" / "dsa" / "sap-xep" / "noi-bot"


def chay(script, stdin, *args):
    p = subprocess.run([sys.executable, str(RUNNER / script), *map(str, args)],
                       input=json.dumps(stdin).encode("utf-8"), capture_output=True, timeout=120)
    return [x for x in p.stdout.decode("utf-8").splitlines() if x.strip()]


def trace_cua(code, du_lieu):
    dong = chay("run.py", {"code": code, "input": du_lieu, "trace": True,
                           "views": [{"v": "a", "kieu": "mang"}]})
    return dong[:-1], json.loads(dong[-1])


def luat(trace, du_lieu):
    return json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": trace}, BAI)[0])


def test_chuan_bi_sinh_du_test_an_va_tb():
    kq = json.loads(chay("chuan_bi.py", {"so_test_an": 10, "time_limit_ms": 1000}, BAI)[0])
    assert kq["t"] == "chuan_bi", kq
    assert [t["id"] for t in kq["tests"]] == [f"an-{i}" for i in range(1, 11)]
    for t in kq["tests"]:
        so = list(map(int, t["input"].split()))
        assert list(map(int, t["output"].split())) == sorted(so)
    assert kq["max_ms"] < 1000, "lời giải mẫu phải chạy dư sức trong giới hạn"
    assert kq["tb"]["input"].strip()


def test_loi_giai_mau_qua_luat_cua_chinh_no():
    tb = json.loads(chay("chuan_bi.py", {"so_test_an": 0}, BAI)[0])["tb"]["input"]
    trace, ket_qua = trace_cua((BAI / "loi_giai.py").read_text(encoding="utf-8"), tb)
    assert not ket_qua["truncated"], "trace của test tb phải ghi đủ"
    assert luat(trace, tb) == {"t": "luat", "co_luat": True, "ok": True, "loi": None}


SAP_XEP_CHON = """\
import viz
a = viz.Mang(list(map(int, input().split())), ten="a")
n = len(a)
for i in range(n):
    m = i
    for j in range(i + 1, n):
        if a[j] < a[m]:
            m = j
    a[i], a[m] = a[m], a[i]
print(*a)
"""

LACH_BANG_SORT = """\
import viz
a = viz.Mang(list(map(int, input().split())), ten="a")
list.__dict__['so' + 'rt'](a)
print(*a)
"""

KHONG_DUNG_MANG = """\
a = list(map(int, input().split()))
for i in range(len(a)):
    for j in range(len(a) - 1 - i):
        if a[j] > a[j + 1]:
            a[j], a[j + 1] = a[j + 1], a[j]
print(*a)
"""


def test_luat_bat_sap_xep_chon_va_cach_lach():
    du_lieu = "5 1 4 2 8 0 3\n"
    for code, chu in ((SAP_XEP_CHON, "kề nhau"), (LACH_BANG_SORT, "chưa sắp xếp xong"),
                      (KHONG_DUNG_MANG, "Không thấy mảng a")):
        trace, ket_qua = trace_cua(code, du_lieu)
        assert ket_qua["stdout"].split() == sorted(du_lieu.split(), key=int)
        kq = luat(trace, du_lieu)
        assert kq["ok"] is False and chu in kq["loi"], (code, kq)


def test_bai_khong_co_luat_thi_dat(tmp_path):
    kq = json.loads(chay("kiem_luat.py", {"input": "", "trace": []}, tmp_path)[0])
    assert kq == {"t": "luat", "co_luat": False, "ok": True}


DAO = RUNNER.parent / "content" / "dsa" / "dslk" / "dao-nguoc"


def test_dao_nguoc_loi_giai_qua_luat_in_nguoc_thi_khong():
    du_lieu = "6\n4 8 15 16 23 42\n"
    loi_giai = (DAO / "loi_giai.py").read_text(encoding="utf-8")
    trace, kq = trace_cua(loi_giai, du_lieu)
    assert kq["stdout"].split() == ["42", "23", "16", "15", "8", "4"]
    assert json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": trace}, DAO)[0])["ok"]

    in_nguoc = (DAO / "khung.py").read_text(encoding="utf-8").replace("print(*ds)", "print(*list(ds)[::-1])")
    trace, kq = trace_cua(in_nguoc, du_lieu)
    assert kq["stdout"].split() == ["42", "23", "16", "15", "8", "4"]
    v = json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": trace}, DAO)[0])
    assert not v["ok"] and "đảo thật" in v["loi"]


BAM = RUNNER.parent / "content" / "dsa" / "bam" / "day-chuyen"


def test_bam_loi_giai_qua_luat_dung_list_rieng_thi_khong():
    du_lieu = (BAM / "demo.in").read_text(encoding="utf-8")
    views = [{"v": "T", "kieu": "bam"}]
    loi_giai = (BAM / "loi_giai.py").read_text(encoding="utf-8")
    dong = chay("run.py", {"code": loi_giai, "input": du_lieu, "trace": True, "views": views})
    assert json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": dong[:-1]}, BAM)[0])["ok"]

    list_rieng = """
m, q = map(int, input().split())
tap = []
for _ in range(q):
    lenh, x = input().split()
    if lenh == "them" and x not in tap:
        tap.append(x)
    elif lenh == "xoa" and x in tap:
        tap.remove(x)
    elif lenh == "tim":
        print("YES" if x in tap else "NO")
"""
    dong = chay("run.py", {"code": list_rieng, "input": du_lieu, "trace": True, "views": views})
    assert json.loads(dong[-1])["stdout"].split() == ["YES", "NO", "YES", "NO"]
    v = json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": dong[:-1]}, BAM)[0])
    assert not v["ok"] and "x mod m" in v["loi"]


FIB = RUNNER.parent / "content" / "dsa" / "de-quy" / "fibonacci"
FIB_VIEWS = [{"v": "fib", "kieu": "cay-goi"}, {"v": "code", "kieu": "code-bien"}]


def luat_fib(code, du_lieu="10\n"):
    dong = chay("run.py", {"code": code, "input": du_lieu, "trace": True, "views": FIB_VIEWS})
    return json.loads(dong[-1]), json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": dong[:-1]}, FIB)[0])


def test_fib_ghi_nho_qua_luat_vong_lap_va_khong_nho_thi_khong():
    kq, v = luat_fib((FIB / "loi_giai.py").read_text(encoding="utf-8"))
    assert kq["stdout"] == "55\n" and v["ok"]

    vong_lap = "def fib(n):\n    a, b = 0, 1\n    for _ in range(n):\n        a, b = b, a + b\n    return a\nprint(fib(int(input())))\n"
    kq, v = luat_fib(vong_lap)
    assert kq["stdout"] == "55\n" and not v["ok"] and "đệ quy" in v["loi"]

    khong_nho = "def fib(n):\n    return n if n < 2 else fib(n - 1) + fib(n - 2)\nprint(fib(int(input())))\n"
    kq, v = luat_fib(khong_nho)
    assert kq["stdout"] == "55\n" and not v["ok"] and "ghi nhớ" in v["loi"]


def luat_bai(bai, views, code, du_lieu=None):
    """Chạy `code` có ghi trace rồi soi bằng luat.py của bài; mặc định dùng đúng test `tb` mà server dùng."""
    if du_lieu is None:
        du_lieu = json.loads(chay("chuan_bi.py", {"so_test_an": 0}, bai)[0])["tb"]["input"]
    dong = chay("run.py", {"code": code, "input": du_lieu, "trace": True, "views": views})
    return json.loads(dong[-1]), json.loads(chay("kiem_luat.py", {"input": du_lieu, "trace": dong[:-1]}, bai)[0])


QUAY_LUI = RUNNER.parent / "content" / "dsa" / "quay-lui"
TONG = QUAY_LUI / "tong-tap-con"
TONG_VIEWS = [{"v": "thu", "kieu": "cay-goi"}, {"v": "a", "kieu": "mang"}]
DAU_TONG = 'import viz\nn, S = map(int, input().split())\na = viz.Mang(sorted(map(int, input().split())), ten="a")\n'


def test_tong_tap_con_cat_nhanh_qua_luat_duyet_het_thi_khong():
    kq, v = luat_bai(TONG, TONG_VIEWS, (TONG / "loi_giai.py").read_text(encoding="utf-8"))
    assert kq["ok"] and v["ok"], v

    duyet_het = DAU_TONG + (
        "def thu(i, tong):\n    if i == n:\n        return 1 if tong == S else 0\n"
        "    return thu(i + 1, tong + a[i]) + thu(i + 1, tong)\nprint(thu(0, 0))\n")
    kq, v = luat_bai(TONG, TONG_VIEWS, duyet_het)
    assert kq["ok"] and not v["ok"] and "cắt nhánh" in v["loi"]

    bit = DAU_TONG + "print(sum(1 for m in range(1 << n) if sum(a[i] for i in range(n) if m >> i & 1) == S))\n"
    kq, v = luat_bai(TONG, TONG_VIEWS, bit)
    assert kq["ok"] and not v["ok"] and "đệ quy" in v["loi"]
