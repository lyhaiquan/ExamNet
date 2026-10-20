"""Chuẩn bị test ẩn của MỘT bài (spec §8.3; phần test của task 3.7). Chạy trong container TIN CẬY.

    python chuan_bi.py <thư mục bài>        # stdin: {"so_test_an": 10, "time_limit_ms": 1000}

Thư mục bài được gắn chỉ đọc. Chỉ container này và container kiểm luật (kiem_luat.py) được gắn thư mục bài;
container chạy code học viên KHÔNG BAO GIỜ được gắn, nếu không code học viên đọc được loi_giai.py.

- Test ẩn `an-1` … `an-N`: input từ `sinh_test.py lon <seed>`, output từ `loi_giai.py` (seed = 1…N).
- Test `tb`: `sinh_test.py tb 1` — server dùng để chạy code học viên có ghi trace rồi kiểm `luat.py`.

In ra một dòng JSON:

    {"t": "chuan_bi", "tests": [{"id": "an-1", "input": "...", "output": "...", "time_ms": 120}, ...],
     "tb": {"input": "...", "output": "..."}, "max_ms": 120}

hoặc {"t": "loi", "loi": "..."} khi bộ sinh test hay lời giải mẫu hỏng (lỗi của người soạn bài).
"""

import json
import os
import subprocess
import sys
import tempfile

from cham import chay_test


def sinh(thu_muc, co, seed):
    p = subprocess.run(
        [sys.executable, os.path.join(thu_muc, "sinh_test.py"), co, str(seed)],
        capture_output=True,
        timeout=20,
        cwd=tempfile.gettempdir(),
    )
    if p.returncode != 0:
        raise RuntimeError(f"sinh_test.py {co} {seed} lỗi: {p.stderr.decode('utf-8', 'replace')[-500:]}")
    return p.stdout.decode("utf-8")


def giai(thu_muc, du_lieu, gioi_han_ms, ten):
    # Lời giải mẫu được rộng giờ gấp 5: chỉ cần đúng, đo thời gian để người soạn biết test có vừa không
    kq = chay_test(os.path.join(thu_muc, "loi_giai.py"), du_lieu, gioi_han_ms * 5)
    if kq["status"] != "OK":
        raise RuntimeError(f"loi_giai.py chạy {ten} bị {kq['status']}: {kq['error']}")
    return kq["stdout"], kq["time_ms"]


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
    thu_muc = os.path.abspath(sys.argv[1])
    job = json.loads(sys.stdin.read() or "{}")
    so_test = int(job.get("so_test_an", 0))
    gioi_han = int(job.get("time_limit_ms", 1000))
    try:
        tests, tb, max_ms = [], None, 0
        if os.path.exists(os.path.join(thu_muc, "sinh_test.py")):
            for seed in range(1, so_test + 1):
                vao = sinh(thu_muc, "lon", seed)
                ra, ms = giai(thu_muc, vao, gioi_han, f"an-{seed}")
                tests.append({"id": f"an-{seed}", "input": vao, "output": ra, "time_ms": ms})
                max_ms = max(max_ms, ms)
            vao = sinh(thu_muc, "tb", 1)
            ra, _ = giai(thu_muc, vao, gioi_han, "tb")
            tb = {"input": vao, "output": ra}
        print(json.dumps({"t": "chuan_bi", "tests": tests, "tb": tb, "max_ms": max_ms}, ensure_ascii=False))
    except Exception as e:  # noqa: BLE001
        print(json.dumps({"t": "loi", "loi": str(e)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
