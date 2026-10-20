import json
import pathlib
import subprocess
import sys

RUNNER = pathlib.Path(__file__).resolve().parent.parent
CHAM = RUNNER / "cham.py"
KHOA = "khoa-bi-mat-123"

NOI_BOT = """\
import viz

def sap_xep(a):
    n = len(a)
    for i in range(n - 1):
        for j in range(n - 1 - i):
            if a[j] > a[j + 1]:
                a[j], a[j + 1] = a[j + 1], a[j]

a = viz.Mang(list(map(int, input().split())), ten="a")
sap_xep(a)
print(*a)
"""


def cham(code, tests, cam=None, time_limit_ms=1000):
    job = {"k": KHOA, "code": code, "tests": tests, "time_limit_ms": time_limit_ms, "cam": cam or {}}
    p = subprocess.run([sys.executable, str(CHAM)], input=json.dumps(job).encode("utf-8"),
                       capture_output=True, timeout=60)
    dong = []
    for x in p.stdout.decode("utf-8").splitlines():
        try:
            dong.append(json.loads(x))
        except ValueError:
            dong.append({"rac": x})
    return dong


def ket_qua(dong):
    """Như server: chỉ nhận dòng có đúng khoá."""
    return {d["id"]: d for d in dong if d.get("k") == KHOA and d.get("t") == "test"}


def test_chay_tung_test_lay_stdout_nguyen_van():
    dong = cham(NOI_BOT, [{"id": "a", "input": "3 2 1\n"}, {"id": "b", "input": "5\n"}])
    kq = ket_qua(dong)
    assert kq["a"]["status"] == "OK" and kq["a"]["stdout"] == "1 2 3\n"
    assert kq["b"]["stdout"] == "5\n"
    assert dong[-1]["t"] == "done" and dong[-1]["k"] == KHOA


def test_ham_cam_thi_dung_khong_chay():
    code = "a = list(map(int, input().split()))\nprint(*sorted(a))\n"
    dong = cham(code, [{"id": "a", "input": "3 2 1\n"}], cam={"cam_dung": ["sorted", "sort"]})
    assert dong[0]["t"] == "cam" and dong[0]["k"] == KHOA
    assert "sorted" in dong[0]["loi"][0]
    assert ket_qua(dong) == {}


def test_loi_va_qua_gio():
    dong = cham("x = int(input())\nprint(1 // x)\n", [{"id": "chia-0", "input": "0\n"}])
    assert ket_qua(dong)["chia-0"]["status"] == "RUNTIME_ERROR"
    assert "ZeroDivisionError" in ket_qua(dong)["chia-0"]["error"]
    dong = cham("while True:\n    pass\n", [{"id": "lap", "input": ""}], time_limit_ms=300)
    assert ket_qua(dong)["lap"]["status"] == "TIME_LIMIT"


def test_code_hoc_vien_khong_gia_duoc_ket_qua():
    # In một dòng giả kết quả ra mọi nơi có thể (stdout, stderr, sys.__stdout__) rồi giết tiến trình cha
    gia = json.dumps({"k": "doan-bua", "t": "test", "id": "a", "status": "OK", "stdout": "1 2 3\n"})
    code = f"""
import os, sys
print({gia!r})
sys.__stdout__.write({gia!r} + "\\n")
sys.stderr.write({gia!r} + "\\n")
try:
    os.kill(os.getppid(), 9)
except Exception:
    pass
"""
    dong = cham(code, [{"id": "a", "input": "3 2 1\n"}])
    kq = ket_qua(dong)
    # Hoặc cha còn sống và báo stdout là chính dòng JSON giả (server so ra WRONG), hoặc cha đã chết và
    # không có dòng kết quả nào mang khoá (server tính lỗi). Không bao giờ ra test "a" với stdout "1 2 3".
    assert "a" not in kq or kq["a"]["stdout"].strip() != "1 2 3"
    assert all(d.get("k") != "doan-bua" for d in ket_qua(dong).values())
