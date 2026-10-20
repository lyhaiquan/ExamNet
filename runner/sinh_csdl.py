"""Sinh database ẩn của MỘT bài SQL (spec §7.6). Chạy trong container TIN CẬY, như chuan_bi.py.

    python sinh_csdl.py <thư mục bài>        # stdin: {"so_csdl": 3}

Gọi `sinh_du_lieu.py <seed>` của bài với seed 1…N; mỗi lần ra một script INSERT (chạy sau schema.sql của
bài). In ra một dòng JSON:

    {"t": "sinh_csdl", "csdl": ["INSERT ...;\n...", ...]}

hoặc {"t": "loi", "loi": "..."} khi bộ sinh dữ liệu hỏng (lỗi của người soạn bài).
"""

import json
import os
import subprocess
import sys
import tempfile


def sinh(thu_muc, seed):
    p = subprocess.run(
        [sys.executable, os.path.join(thu_muc, "sinh_du_lieu.py"), str(seed)],
        capture_output=True,
        timeout=20,
        cwd=tempfile.gettempdir(),
    )
    if p.returncode != 0:
        raise RuntimeError(f"sinh_du_lieu.py {seed} lỗi: {p.stderr.decode('utf-8', 'replace')[-500:]}")
    return p.stdout.decode("utf-8")


def main():
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    thu_muc = os.path.abspath(sys.argv[1])
    job = json.loads(sys.stdin.read() or "{}")
    n = int(job.get("so_csdl", 3))
    try:
        csdl = [sinh(thu_muc, seed) for seed in range(1, n + 1)]
        print(json.dumps({"t": "sinh_csdl", "csdl": csdl}, ensure_ascii=False))
    except Exception as e:  # noqa: BLE001 — mọi lỗi của bài đều báo về server
        print(json.dumps({"t": "loi", "loi": str(e)}, ensure_ascii=False))


if __name__ == "__main__":
    main()
