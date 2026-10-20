"""sinh_csdl.py: 3 database ẩn của bài SQL chạy được trên schema của bài, đúng tính chất spec §7.6."""

import json
import os
import sqlite3
import subprocess
import sys

GOC = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
RUNNER = os.path.join(GOC, "runner")
BAI = os.path.join(GOC, "content", "sql", "null-case", "chua-co-lop")


def chay():
    p = subprocess.run(
        [sys.executable, os.path.join(RUNNER, "sinh_csdl.py"), BAI],
        input=b'{"so_csdl": 3}',
        capture_output=True,
        timeout=60,
    )
    return json.loads(p.stdout.decode("utf-8").strip().splitlines()[-1])


def dung(script):
    con = sqlite3.connect(":memory:")
    with open(os.path.join(BAI, "schema.sql"), encoding="utf-8") as f:
        con.executescript(f.read())
    con.executescript(script)
    return con


def test_ba_csdl_chay_duoc_va_dung_tinh_chat():
    out = chay()
    assert out["t"] == "sinh_csdl", out
    assert len(out["csdl"]) == 3
    c1, c2, c3 = (dung(s) for s in out["csdl"])
    # seed 1: có sinh viên chưa xếp lớp, vắng thi, ngày sinh đúng mốc 2005-01-01
    assert c1.execute("SELECT COUNT(*) FROM sinh_vien WHERE lop_id IS NULL").fetchone()[0] > 0
    assert c1.execute("SELECT COUNT(*) FROM ket_qua WHERE diem IS NULL").fetchone()[0] > 0
    assert c1.execute("SELECT COUNT(*) FROM sinh_vien WHERE ngay_sinh = '2005-01-01'").fetchone()[0] > 0
    # seed 2: có họ tên trùng
    assert c2.execute("SELECT MAX(n) FROM (SELECT COUNT(*) AS n FROM sinh_vien GROUP BY ho_ten)").fetchone()[0] > 1
    # seed 3: một bảng rỗng và một lớp không có sinh viên
    assert c3.execute("SELECT COUNT(*) FROM ket_qua").fetchone()[0] == 0
    assert c3.execute(
        "SELECT COUNT(*) FROM lop WHERE id NOT IN (SELECT lop_id FROM sinh_vien WHERE lop_id IS NOT NULL)"
    ).fetchone()[0] > 0


def test_cung_seed_cung_du_lieu():
    assert chay() == chay()
