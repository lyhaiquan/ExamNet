"""Chạy một bài làm Python và in trace (spec §8.1, §8.2).

Đọc từ stdin một object JSON:

    {"code": "...", "input": "5 1 4 2\\n", "trace": true, "time_limit_ms": 1000,
     "max_events": 10000, "max_bytes": 2000000, "lesson": "dsa.sap-xep.noi-bot",
     "views": [{"v": "a", "kieu": "mang"}]}

In ra stdout, mỗi dòng một JSON: các sự kiện trace (nếu "trace"), rồi đúng MỘT dòng kết quả cuối

    {"t": "result", "ok": true, "stdout": "1 2 4 5\\n", "error": null, "truncated": false, "time_ms": 3}

`"ngon_ngu"` khác `"python"` (c, cpp, java) thì chuyển cho ngon_ngu.py: biên dịch, chạy, chạy từng dòng dưới
debugger; cùng định dạng dòng kết quả.

Thời gian bị giới hạn hai lớp: RLIMIT_CPU ở đây (Linux, trong Docker) và tiến trình Java giết container
khi quá hạn.
"""

import io
import json
import math
import sys
import time

import goi_y
import ngon_ngu
from viz import cay_goi, code_bien, recorder, theo_doi, tung_buoc

MAX_STDOUT = 1_000_000  # ký tự; in nhiều hơn thì cắt, tránh tràn bộ nhớ


class GioiHanStdout(io.StringIO):
    """Gom phần học viên print, cắt khi quá MAX_STDOUT."""

    def __init__(self):
        super().__init__()
        self.cat = False
        self.byte_count = 0  # Mốc UTF-8 đã thực sự được ghi vào stdout của học viên.

    def write(self, s):
        con = MAX_STDOUT - self.tell()
        if con <= 0:
            self.cat = True
            return len(s)
        if len(s) > con:
            self.cat = True
            s = s[:con]
        self.byte_count += len(s.encode("utf-8"))
        return super().write(s)


def gioi_han_cpu(time_limit_ms):
    try:
        import resource  # chỉ có trên Linux/macOS
    except ImportError:
        return
    giay = max(1, math.ceil(time_limit_ms / 1000)) + 1
    resource.setrlimit(resource.RLIMIT_CPU, (giay, giay + 1))


def loi_rut_gon(exc, code):
    """Traceback chỉ giữ các khung của bai_lam.py, kèm dòng code tương ứng."""
    dong_code = code.splitlines()
    if isinstance(exc, SyntaxError) and exc.filename == "bai_lam.py":
        noi_dung = (exc.text or "").rstrip()
        return f"Lỗi cú pháp ở dòng {exc.lineno}: {exc.msg}\n    {noi_dung}"
    import traceback

    phan = []
    for khung in traceback.extract_tb(exc.__traceback__):
        if khung.filename == "bai_lam.py":
            n = khung.lineno or 0
            src = dong_code[n - 1].strip() if 0 < n <= len(dong_code) else ""
            phan.append(f'  File "bai_lam.py", dòng {n}, trong {khung.name}\n    {src}')
    dau = "Traceback (bai_lam.py):\n" if phan else ""
    return dau + "\n".join(phan) + ("\n" if phan else "") + f"{type(exc).__name__}: {exc}"


def chay(job, out):
    co_trace = bool(job.get("trace"))
    if co_trace:
        out.write(json.dumps(
            {"t": "hdr", "lesson": job.get("lesson", ""), "views": job.get("views", []),
             "source": job.get("code", "") if job.get("line_trace") else ""},
            ensure_ascii=False) + "\n")
        recorder.current = recorder.Recorder(
            out=out,
            max_events=int(job.get("max_events", 10_000)),
            max_bytes=int(job.get("max_bytes", 2_000_000)))
    else:
        recorder.current = recorder.Recorder(out=None, enabled=False)

    theo_doi.tat()
    cay_goi.tat()
    tung_buoc.tat()
    code = job.get("code", "")
    buoc = None
    if co_trace:
        # bộ vẽ không gắn với cấu trúc nào trong code: cây lời gọi (bien là tên hàm) và chạy từng dòng
        for v in job.get("views", []):
            if v.get("kieu") == "cay-goi":
                cay_goi.bat(v.get("v", ""))
            elif v.get("kieu") == "code-bien":
                code_bien.bat(v.get("v", "code"), code, max_lines=800 if job.get("line_trace") else None)
            elif v.get("kieu") == "tung-dong":
                buoc = v.get("v", "buoc")  # bật ngay trước exec: không ghi các dòng của run.py
    if co_trace and job.get("line_trace") and buoc is None and not any(
        v.get("kieu") == "code-bien" for v in job.get("views", [])
    ):
        # Debug opt-in: sinh sự kiện line/vars thật, không ảnh hưởng lần Chạy thử thông thường.
        # Giới hạn 800 dòng để vòng lặp dài không làm nổ trace; chương trình vẫn chạy tiếp.
        # Chạy từng bước cũng dùng sys.settrace nên không bật cùng lúc.
        code_bien.bat("__trace_table__", code, max_lines=800)
    stdout_hv = GioiHanStdout()
    stdin_goc, stdout_goc = sys.stdin, sys.stdout
    sys.stdin = io.StringIO(job.get("input", ""))
    sys.stdout = stdout_hv
    ok, loi, cat_trace, chi_tiet = True, None, False, None
    bat_dau = time.perf_counter()
    try:
        bien_dich = compile(code, "bai_lam.py", "exec")
        if buoc is not None:
            tung_buoc.bat(buoc, code, int(job.get("max_steps", tung_buoc.TRAN)))
        exec(bien_dich, {"__name__": "__main__"})
    except recorder.TraceLimit:
        cat_trace = True
    except SystemExit as e:
        if e.code not in (None, 0):
            ok, loi = False, f"Chương trình thoát với mã {e.code}"
    except BaseException as e:  # noqa: BLE001 — mọi lỗi của bài làm đều phải báo về
        tung_buoc.ngung()
        chi_tiet = goi_y.thong_tin(e, code)
        ok, loi = False, loi_rut_gon(e, code) + (f"\nGợi ý: {chi_tiet['goi_y']}" if chi_tiet["goi_y"] else "")
    finally:
        tung_buoc.ngung()
        theo_doi.tat()
        cay_goi.tat()
        sys.stdin, sys.stdout = stdin_goc, stdout_goc
    thoi_gian = round((time.perf_counter() - bat_dau) * 1000)

    if co_trace and buoc is not None and not cat_trace:
        # chạy từng bước: dòng lỗi có cấu trúc (app tô đỏ dòng, không phải đọc chữ) và output cuối
        try:
            if chi_tiet is not None:
                recorder.current.emit({"t": "loi", "v": buoc, **chi_tiet})
            tung_buoc.xong(stdout_hv.getvalue(), ok)
        except recorder.TraceLimit:
            cat_trace = True
    tung_buoc.tat()
    if co_trace:
        out.write(json.dumps({"t": "end", "truncated": cat_trace}) + "\n")
    ket_qua = {
        "t": "result",
        "ok": ok,
        "stdout": stdout_hv.getvalue(),
        "error": loi,
        "truncated": cat_trace,
        "stdout_truncated": stdout_hv.cat,
        "time_ms": thoi_gian,
    }
    if chi_tiet is not None:
        ket_qua["error_line"], ket_qua["error_kind"] = chi_tiet["n"], chi_tiet["kieu"]
    out.write(json.dumps(ket_qua, ensure_ascii=False) + "\n")
    out.flush()


def main():
    # stdout và stdin luôn UTF-8, kể cả trên Windows (chế độ không sandbox)
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdin.reconfigure(encoding="utf-8")
    job = json.loads(sys.stdin.read())
    if job.get("ngon_ngu", "python") != "python":
        # C/C++/Java: giới hạn CPU đặt riêng cho tiến trình chương trình, không cho trình biên dịch hay debugger
        ngon_ngu.chay_job(job, sys.stdout)
        return
    gioi_han_cpu(int(job.get("time_limit_ms", 1000)))
    chay(job, sys.stdout)


if __name__ == "__main__":
    main()
