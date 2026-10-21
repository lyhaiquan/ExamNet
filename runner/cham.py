"""Chấm bài nộp: kiểm hàm cấm, rồi chạy từng test trong một tiến trình con mới (spec §8.2).

Đọc từ stdin một object JSON:

    {"k": "<khoá bí mật của lượt chấm>", "code": "...", "time_limit_ms": 1000, "ngon_ngu": "python",
     "tests": [{"id": "nho-1", "input": "3 2 1\\n"}, ...],
     "cam": {"cam_dung": ["sorted", "sort"], "cam_import": [], "cam_cu_phap": []}}

In ra stdout, mỗi dòng một JSON, dòng nào cũng kèm "k":

    {"k": "...", "t": "cam", "loi": ["Không được dùng sorted (dòng 4)"]}      # dùng hàm cấm: dừng luôn
    {"k": "...", "t": "bien_dich", "loi": "Lỗi biên dịch: ...", "dong": 3}     # C/C++/Java không biên dịch được
    {"k": "...", "t": "test", "id": "nho-1", "status": "OK", "time_ms": 41, "stdout": "...", "error": null}
    {"k": "...", "t": "done"}

`status` chỉ là chạy xong (OK), lỗi (RUNTIME_ERROR) hay quá giờ (TIME_LIMIT). Đúng/sai do server so, ở
NGOÀI container: output mong đợi không bao giờ vào đây.

Vì sao chắc kết quả không bị giả:
- Code học viên chạy ở tiến trình con; stdout của nó là ống nối về đây, không phải stdout của container.
- Khoá "k" chỉ nằm trong bộ nhớ tiến trình này (không ở argv hay biến môi trường của con); dòng không có
  khoá đúng thì server bỏ qua. Tiến trình này tự đặt "không dump được" để con không đọc được bộ nhớ hay
  mở /proc/<cha>/fd của nó.
- Con giết cha (os.kill) thì thiếu dòng kết quả, server tính test đó là lỗi chứ không phải đúng.
- Thời gian đo ở đây (gồm cả khởi động Python), không tin con tự báo.
"""

import json
import os
import subprocess
import sys
import tempfile
import time

import kiem_cam
import ngon_ngu as ngon_ngu_mod

THU_MUC = os.path.dirname(os.path.abspath(__file__))
CHAY_MOT = os.path.join(THU_MUC, "chay_mot.py")
MAX_STDOUT = 1_000_000  # byte
MAX_ERROR = 2_000  # ký tự
NOI_LONG_MS = 1_000  # chờ thêm ngoài giới hạn trước khi giết tiến trình con


def khong_dump_duoc():
    """prctl(PR_SET_DUMPABLE, 0): tiến trình cùng uid không đọc được bộ nhớ hay fd của tiến trình này."""
    try:
        import ctypes

        ctypes.CDLL(None).prctl(4, 0, 0, 0, 0)
    except Exception:  # noqa: BLE001 — không phải Linux (chế độ không sandbox trên Windows)
        pass


def chay_test(duong_dan_code, du_lieu, gioi_han_ms):
    env = {
        "PYTHONIOENCODING": "utf-8",
        "PYTHONDONTWRITEBYTECODE": "1",
        "LABCAST_TIME_LIMIT_MS": str(gioi_han_ms),
    }
    if os.name == "nt":  # Windows cần SYSTEMROOT để Python khởi động
        env["SYSTEMROOT"] = os.environ.get("SYSTEMROOT", "C:\\Windows")
    bat_dau = time.perf_counter()
    try:
        p = subprocess.run(
            [sys.executable, CHAY_MOT, os.path.abspath(duong_dan_code)],
            input=du_lieu.encode("utf-8"),
            capture_output=True,
            timeout=(gioi_han_ms + NOI_LONG_MS) / 1000,
            close_fds=True,
            env=env,
            cwd=tempfile.gettempdir(),
        )
    except subprocess.TimeoutExpired:
        return {"status": "TIME_LIMIT", "time_ms": round((time.perf_counter() - bat_dau) * 1000),
                "stdout": "", "error": None}
    ms = round((time.perf_counter() - bat_dau) * 1000)
    if p.returncode == 0:
        status = "TIME_LIMIT" if ms > gioi_han_ms else "OK"
        loi = None
    else:
        status = "TIME_LIMIT" if ms > gioi_han_ms else "RUNTIME_ERROR"
        loi = p.stderr.decode("utf-8", "replace")[-MAX_ERROR:].strip() or f"Chương trình thoát với mã {p.returncode}"
    return {"status": status, "time_ms": ms,
            "stdout": p.stdout[:MAX_STDOUT].decode("utf-8", "replace"), "error": loi}


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
    job = json.loads(sys.stdin.read())
    khoa = job["k"]
    khong_dump_duoc()

    def in_ra(obj):
        obj["k"] = khoa
        sys.stdout.write(json.dumps(obj, ensure_ascii=False) + "\n")
        sys.stdout.flush()

    code = job.get("code", "")
    cam = job.get("cam", {})
    if job.get("ngon_ngu", "python") != "python":
        cham_ngon_ngu(job, in_ra)
        return
    loi = kiem_cam.kiem(code, cam.get("cam_dung", ()), cam.get("cam_import", ()), cam.get("cam_cu_phap", ()))
    if loi:
        in_ra({"t": "cam", "loi": loi})
        return

    gioi_han = int(job.get("time_limit_ms", 1000))
    with tempfile.TemporaryDirectory() as thu_muc:
        duong_dan = os.path.join(thu_muc, "bai_lam.py")
        with open(duong_dan, "w", encoding="utf-8") as f:
            f.write(code)
        for t in job.get("tests", []):
            kq = chay_test(duong_dan, t.get("input", ""), gioi_han)
            kq.update({"t": "test", "id": t["id"]})
            in_ra(kq)
    in_ra({"t": "done"})


def cham_ngon_ngu(job, in_ra):
    """C/C++/Java: kiểm tên cấm, biên dịch MỘT lần, chạy từng test ở tiến trình riêng như bài Python."""
    ngon_ngu = job["ngon_ngu"]
    code = job.get("code", "")
    cam = job.get("cam", {})
    ds_cam = kiem_cam.cam_cua(ngon_ngu, cam.get("cam_dung", ()), cam.get("cam_import", ()),
                              cam.get("cam_theo_ngon_ngu"))
    loi = kiem_cam.kiem_ngon_ngu(code, ngon_ngu, ds_cam)
    if loi:
        in_ra({"t": "cam", "loi": loi})
        return
    gioi_han = int(job.get("time_limit_ms", 1000))
    with ngon_ngu_mod.ThuMuc(ngon_ngu, code) as tm:
        ok, loi_bd, dong = tm.bien_dich()
        if not ok:
            in_ra({"t": "bien_dich", "loi": loi_bd, "dong": dong})
            return
        for t in job.get("tests", []):
            kq = ngon_ngu_mod.chay(tm, t.get("input", ""), gioi_han)
            kq.pop("stdout_truncated", None)
            kq.pop("error_line", None)
            kq.update({"t": "test", "id": t["id"]})
            in_ra(kq)
    in_ra({"t": "done"})


if __name__ == "__main__":
    main()
