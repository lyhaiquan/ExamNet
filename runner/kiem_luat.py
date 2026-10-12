"""Kiểm luật soi trace của bài (`luat.py`) trên trace code học viên (spec §8.2 bước 3). Chạy trong container
TIN CẬY có gắn thư mục bài; KHÔNG chạy code học viên, chỉ đọc trace của nó.

    python kiem_luat.py <thư mục bài>        # stdin: {"input": "...", "trace": ["<dòng JSON>", ...]}

`luat.py` của bài định nghĩa:

    def kiem(su_kien: list[dict], dau_vao: str) -> str | None   # None = đạt, chuỗi = lý do vi phạm

In ra một dòng: {"t": "luat", "co_luat": true, "ok": false, "loi": "..."}. luat.py tự lỗi (lỗi của người soạn
bài) thì {"t": "luat", "co_luat": true, "ok": true, "loi_he_thong": "..."}: không phạt học viên vì bài hỏng.
"""

import importlib.util
import json
import os
import sys


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
    duong_dan = os.path.join(sys.argv[1], "luat.py")
    job = json.loads(sys.stdin.read() or "{}")
    if not os.path.exists(duong_dan):
        print(json.dumps({"t": "luat", "co_luat": False, "ok": True}))
        return
    su_kien = []
    for dong in job.get("trace", []):
        try:
            su_kien.append(json.loads(dong))
        except (ValueError, TypeError):
            continue  # dòng hỏng: bỏ
    try:
        spec = importlib.util.spec_from_file_location("luat", duong_dan)
        luat = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(luat)
        loi = luat.kiem(su_kien, job.get("input", ""))
        print(json.dumps({"t": "luat", "co_luat": True, "ok": loi is None, "loi": loi}, ensure_ascii=False))
    except Exception as e:  # noqa: BLE001
        print(json.dumps({"t": "luat", "co_luat": True, "ok": True, "loi_he_thong": f"{type(e).__name__}: {e}"},
                         ensure_ascii=False))


if __name__ == "__main__":
    main()
