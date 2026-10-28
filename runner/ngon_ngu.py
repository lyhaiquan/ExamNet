"""Biên dịch và chạy bài làm C, C++, Java (spec đa ngôn ngữ §5). Python đi đường cũ trong run.py/cham.py.

Mỗi lượt làm việc trong một thư mục tạm riêng: ghi code, biên dịch (không tính giờ), rồi chạy chương trình
với giới hạn CPU đặt riêng cho tiến trình chương trình. Chạy từng dòng thì biên dịch kèm thông tin debug và
chạy dưới debugger (buoc/gdb_buoc.py cho C/C++, buoc/JdiBuoc.java cho Java), debugger ghi sự kiện
`line`/`vars` ra file, run.py đọc lại và in như trace của Python.
"""

import json
import math
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time

THU_MUC = os.path.dirname(os.path.abspath(__file__))
THU_MUC_BUOC = os.path.join(THU_MUC, "buoc")
MAX_STDOUT = 1_000_000  # byte
MAX_LOI = 2_000  # ký tự
MAX_DONG_LOI_BIEN_DICH = 12
NOI_LONG_MS = 1_000  # chờ thêm ngoài giới hạn trước khi giết chương trình
TRAN_BUOC = 800  # chạy từng dòng: tối đa bấy nhiêu dòng, quá thì chương trình chạy nốt không ghi

# file: tên file học viên; bien_dich / bien_dich_debug: lệnh, {src} {out} {dir} thay lúc chạy;
# he_so_gio: nhân giới hạn thời gian (JVM khởi động chậm)
NGON_NGU = {
    "c": {
        "file": "main.c",
        "bien_dich": ["gcc", "-std=c17", "-O2", "-pipe", "-fdiagnostics-color=never", "-o", "{out}", "{src}", "-lm"],
        "bien_dich_debug": ["gcc", "-std=c17", "-g", "-O0", "-fdiagnostics-color=never", "-o", "{out}", "{src}", "-lm"],
        "he_so_gio": 1,
    },
    "cpp": {
        "file": "main.cpp",
        "bien_dich": ["g++", "-std=c++17", "-O2", "-pipe", "-fdiagnostics-color=never", "-o", "{out}", "{src}"],
        "bien_dich_debug": ["g++", "-std=c++17", "-g", "-O0", "-fdiagnostics-color=never", "-o", "{out}", "{src}"],
        "he_so_gio": 1,
    },
    "java": {
        "file": "Main.java",
        "bien_dich": ["javac", "-J-Xshare:auto", "-J-XX:+UseSerialGC", "-J-XX:TieredStopAtLevel=1",
                     "--release", "17", "-encoding", "UTF-8", "-d", "{dir}", "{src}"],
        "bien_dich_debug": ["javac", "-J-Xshare:auto", "-J-XX:+UseSerialGC", "-J-XX:TieredStopAtLevel=1",
                            "--release", "17", "-g", "-encoding", "UTF-8", "-d", "{dir}", "{src}"],
        "he_so_gio": 2,
    },
}

# JVM chạy bài làm: ít luồng (container giới hạn 64 tiến trình/luồng), không ghi hsperfdata, ngăn xếp sâu
# cho đệ quy
JVM_CO = ["-Xss64m", "-Xmx192m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=1",
          "-XX:-UsePerfData", "-Xshare:auto"]

GOI_Y_JAVA = {
    "ArrayIndexOutOfBoundsException": "truy cập ngoài mảng: kiểm lại chỉ số và điều kiện dừng vòng lặp",
    "StringIndexOutOfBoundsException": "truy cập ngoài chuỗi: kiểm lại chỉ số",
    "IndexOutOfBoundsException": "truy cập ngoài danh sách: kiểm lại chỉ số và kích thước",
    "NullPointerException": "dùng biến đang là null: kiểm lại chỗ khởi tạo đối tượng hoặc mảng",
    "ArithmeticException": "lỗi số học, thường là chia hoặc lấy dư cho 0",
    "StackOverflowError": "đệ quy quá sâu hoặc không có điểm dừng",
    "OutOfMemoryError": "dùng quá nhiều bộ nhớ",
    "NumberFormatException": "đổi chuỗi sang số không được: kiểm lại cách tách input",
    "NoSuchElementException": "đọc input khi đã hết: kiểm lại số lần đọc",
    "InputMismatchException": "kiểu input không khớp với lệnh đọc",
    "NegativeArraySizeException": "tạo mảng với kích thước âm",
}

# Xử lý an toàn các tín hiệu (chỉ nạp các tín hiệu tồn tại trên hệ điều hành hiện tại, ví dụ Windows không có SIGBUS)
_TIN_HIEU_THO = {
    "SIGSEGV": "Lỗi truy cập bộ nhớ (Segmentation fault): thường do truy cập ngoài mảng, con trỏ chưa khởi tạo hoặc đệ quy quá sâu",
    "SIGFPE": "Lỗi số học (Floating point exception): thường là chia hoặc lấy dư cho 0",
    "SIGABRT": "Chương trình bị huỷ (abort): thường do assert sai, cấp phát lỗi hoặc ngoại lệ không bắt",
    "SIGBUS": "Lỗi truy cập bộ nhớ (Bus error)",
    "SIGILL": "Lệnh không hợp lệ (Illegal instruction): thường do chia cho 0 hoặc hàm có kiểu trả về mà thiếu return",
}

TIN_HIEU = {}
for _ten, _mo_ta in _TIN_HIEU_THO.items():
    if hasattr(signal, _ten):
        TIN_HIEU[getattr(signal, _ten)] = _mo_ta


def ho_tro(ngon_ngu):
    return ngon_ngu in NGON_NGU


class ThuMuc:
    """Thư mục tạm của một lượt: có code, file chạy, file input/output."""

    def __init__(self, ngon_ngu, code):
        self.ngon_ngu = ngon_ngu
        self.cau_hinh = NGON_NGU[ngon_ngu]
        self.duong_dan = tempfile.mkdtemp(prefix="lc-")
        self.src = os.path.join(self.duong_dan, self.cau_hinh["file"])
        self.out = os.path.join(self.duong_dan, "main")
        with open(self.src, "w", encoding="utf-8", newline="\n") as f:
            f.write(code)

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        shutil.rmtree(self.duong_dan, ignore_errors=True)

    def file(self, ten):
        return os.path.join(self.duong_dan, ten)

    def bien_dich(self, debug=False):
        """Trả (thành công, thông báo lỗi, dòng lỗi đầu tiên hoặc None)."""
        lenh = [
            p.replace("{src}", self.src).replace("{out}", self.out).replace("{dir}", self.duong_dan)
            for p in self.cau_hinh["bien_dich_debug" if debug else "bien_dich"]
        ]
        try:
            p = subprocess.run(lenh, capture_output=True, timeout=30, cwd=self.duong_dan, env=_moi_truong())
        except FileNotFoundError:
            return False, f"Máy chạy bài chưa cài trình biên dịch {lenh[0]}. Báo giáo viên dùng Docker sandbox.", None
        except subprocess.TimeoutExpired:
            return False, "Biên dịch quá 30 giây", None
        if p.returncode == 0:
            return True, None, None
        return (False, *_rut_gon_loi_bien_dich(p.stderr.decode("utf-8", "replace"), self))

    def lenh_chay(self):
        if self.ngon_ngu == "java":
            return [_java(), *JVM_CO, "-cp", self.duong_dan, "Main"]
        return [self.out]


def _java():
    home = os.environ.get("JAVA_HOME")
    if home and os.path.exists(os.path.join(home, "bin", "java")):
        return os.path.join(home, "bin", "java")
    return "java"


def _moi_truong():
    env = {"PATH": os.environ.get("PATH", "/usr/local/bin:/usr/bin:/bin"), "LANG": "C.UTF-8",
           "TMPDIR": tempfile.gettempdir(), "HOME": tempfile.gettempdir()}
    if os.environ.get("JAVA_HOME"):
        env["JAVA_HOME"] = os.environ["JAVA_HOME"]
    if os.name == "nt":
        env["SYSTEMROOT"] = os.environ.get("SYSTEMROOT", "C:\\Windows")
    return env


def _rut_gon_loi_bien_dich(stderr, tm):
    ten = tm.cau_hinh["file"]
    stderr = stderr.replace(tm.src, ten).replace(tm.duong_dan + os.sep, "")
    dong_loi = None
    giu = []
    for dong in stderr.splitlines():
        m = re.match(re.escape(ten) + r":(\d+)(?::\d+)?: (?:fatal )?error", dong)
        if m and dong_loi is None:
            dong_loi = int(m.group(1))
        if dong.strip() and not dong.startswith("compilation terminated"):
            giu.append(dong)
    if len(giu) > MAX_DONG_LOI_BIEN_DICH:
        giu = giu[:MAX_DONG_LOI_BIEN_DICH] + ["…"]
    loi = "Lỗi biên dịch:\n" + "\n".join(giu)
    if tm.ngon_ngu == "java" and re.search(r"class \w+ is public, should be declared in a file named", stderr):
        loi += "\nGợi ý: lớp public chứa hàm main phải tên là Main (public class Main)."
    return loi[:MAX_LOI], dong_loi


def _gioi_han(gioi_han_ms, ngon_ngu):
    """preexec_fn của tiến trình chương trình: giới hạn CPU và nới ngăn xếp (đệ quy sâu)."""

    def dat():
        try:
            import resource
        except ImportError:
            return
        giay = max(1, math.ceil(gioi_han_ms / 1000)) + 1
        resource.setrlimit(resource.RLIMIT_CPU, (giay, giay + 1))
        if ngon_ngu != "java":
            _, cung = resource.getrlimit(resource.RLIMIT_STACK)
            muon = 256 * 1024 * 1024
            if cung == resource.RLIM_INFINITY or cung >= muon:
                resource.setrlimit(resource.RLIMIT_STACK, (muon, cung))

    return dat if os.name != "nt" else None


def gio_cua(ngon_ngu, gioi_han_ms):
    return gioi_han_ms * NGON_NGU[ngon_ngu]["he_so_gio"]


def chuan_hoa_stdout(stdout):
    """Chuẩn hóa xuống dòng Windows/Linux, giữ nguyên nội dung."""
    return stdout.replace("\r\n", "\n")


def la_loi_het_cpu(ma):
    for ten in ("SIGXCPU", "SIGKILL"):
        sig = getattr(signal, ten, None)
        if sig is not None and ma in (-sig, 128 + sig):
            return True
    return False


def chay(tm, du_lieu, gioi_han_ms):
    """Chạy file đã biên dịch với một input. Trả dict status/time_ms/stdout/error/error_line như cham.py."""
    gioi_han = gio_cua(tm.ngon_ngu, gioi_han_ms)
    bat_dau = time.perf_counter()
    try:
        p = subprocess.run(
            tm.lenh_chay(),
            input=du_lieu.encode("utf-8"),
            capture_output=True,
            timeout=(gioi_han + NOI_LONG_MS) / 1000,
            close_fds=True,
            cwd=tm.duong_dan,
            env=_moi_truong(),
            preexec_fn=_gioi_han(gioi_han, tm.ngon_ngu),
        )
    except subprocess.TimeoutExpired:
        return {"status": "TIME_LIMIT", "time_ms": round((time.perf_counter() - bat_dau) * 1000),
                "stdout": "", "error": None, "error_line": None}
    ms = round((time.perf_counter() - bat_dau) * 1000)
    stdout = chuan_hoa_stdout(
        p.stdout[:MAX_STDOUT].decode("utf-8", "replace")
    )
    if p.returncode == 0:
        return {"status": "TIME_LIMIT" if ms > gioi_han else "OK", "time_ms": ms, "stdout": stdout,
                "error": None, "error_line": None, "stdout_truncated": len(p.stdout) > MAX_STDOUT}
    het_cpu = la_loi_het_cpu(p.returncode)
    if ms > gioi_han or het_cpu:
        return {"status": "TIME_LIMIT", "time_ms": ms, "stdout": stdout, "error": None, "error_line": None}
    loi, dong = loi_chay(tm.ngon_ngu, p.returncode, p.stderr.decode("utf-8", "replace"))
    return {"status": "RUNTIME_ERROR", "time_ms": ms, "stdout": stdout, "error": loi, "error_line": dong}


def loi_chay(ngon_ngu, ma, stderr):
    """Thông báo lỗi lúc chạy (tiếng Việt) và dòng lỗi nếu biết."""
    # JVM in dòng này khi máy đặt JAVA_TOOL_OPTIONS (proxy, chứng chỉ): không phải lỗi của bài làm
    stderr = "\n".join(d for d in stderr.splitlines() if not d.startswith("Picked up JAVA_TOOL_OPTIONS")).strip()
    if ngon_ngu == "java":
        return _loi_java(stderr, ma)
    if ma < 0 and -ma in TIN_HIEU:
        loi = TIN_HIEU[-ma]
    else:
        loi = f"Chương trình thoát với mã {ma}"
    if stderr:
        loi += "\n" + stderr[-MAX_LOI:]
    return loi[:MAX_LOI], None


def _loi_java(stderr, ma):
    dong_loi = None
    giu = []
    lop = None
    for dong in stderr.splitlines():
        if dong.startswith("Exception in thread") or dong.startswith("Caused by") or not dong.startswith("\t"):
            giu.append(dong.replace('Exception in thread "main" ', ""))
            m = re.search(r"([A-Za-z]+(?:Exception|Error))", dong)
            if m and lop is None:
                lop = m.group(1)
        elif "(Main.java:" in dong:
            giu.append("    " + dong.strip())
            m = re.search(r"\(Main\.java:(\d+)\)", dong)
            if m and dong_loi is None:
                dong_loi = int(m.group(1))
    loi = "\n".join(giu[:12]) if giu else f"Chương trình thoát với mã {ma}"
    if lop in GOI_Y_JAVA:
        loi += f"\nGợi ý: {GOI_Y_JAVA[lop]}."
    return loi[:MAX_LOI], dong_loi


# ---- chạy từng dòng ----------------------------------------------------------------------------------


def chay_tung_dong(tm, du_lieu, gioi_han_ms, tran=TRAN_BUOC):
    """Chạy dưới debugger. Trả (sự kiện line/vars, kết quả như chay(), bị cắt hay không).

    Chương trình đã biên dịch kèm debug (tm.bien_dich(debug=True)).
    """
    f_vao, f_ra, f_trace, f_meta = (tm.file(t) for t in ("vao.txt", "ra.txt", "trace.jsonl", "meta.json"))
    with open(f_vao, "w", encoding="utf-8", newline="\n") as f:
        f.write(du_lieu)
    gioi_han = gio_cua(tm.ngon_ngu, gioi_han_ms)
    # debugger dừng ở mỗi dòng: chậm hơn chạy thường nhiều lần; trần dòng giữ thời gian trong vài giây
    han = (gioi_han + 15_000) / 1000
    if tm.ngon_ngu == "java":
        # hai JVM (debugger và bài làm) chung giới hạn RAM của container: thu nhỏ heap của cả hai
        jvm_bai = [c if not c.startswith("-Xmx") else "-Xmx128m" for c in JVM_CO]
        lenh = [_java(), "-Xmx48m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=1",
                "-XX:-UsePerfData", "-Xshare:auto", "-cp", _lop_jdi(), "JdiBuoc", f_vao, f_ra, f_trace, f_meta,
                str(tran), " ".join(jvm_bai)]
        env = _moi_truong()
        env["CLASSPATH"] = tm.duong_dan  # JVM bài làm tìm Main ở đây (đường dẫn có thể có dấu cách)
    else:
        lenh = ["gdb", "-q", "-nx", "-batch", "-x", os.path.join(THU_MUC_BUOC, "gdb_buoc.py"), "--args", tm.out]
        env = _moi_truong()
        env.update({"LC_SRC": tm.cau_hinh["file"], "LC_IN": f_vao, "LC_OUT": f_ra, "LC_TRACE": f_trace,
                    "LC_META": f_meta, "LC_MAX": str(tran)})
    bat_dau = time.perf_counter()
    try:
        # giới hạn CPU đặt cho debugger thì chương trình (tiến trình con của nó) kế thừa; +2 s cho chính debugger
        p = subprocess.run(lenh, capture_output=True, timeout=han, cwd=tm.duong_dan, env=env, close_fds=True,
                           preexec_fn=_gioi_han(gioi_han + 2_000, tm.ngon_ngu))
        het_gio = False
    except subprocess.TimeoutExpired:
        p, het_gio = None, True
    ms = round((time.perf_counter() - bat_dau) * 1000)

    su_kien = []
    if os.path.exists(f_trace):
        with open(f_trace, encoding="utf-8", errors="replace") as f:
            for dong in f:
                try:
                    su_kien.append(json.loads(dong))
                except ValueError:
                    break  # dòng cuối viết dở khi bị giết
    meta = {}
    if os.path.exists(f_meta):
        try:
            with open(f_meta, encoding="utf-8") as f:
                meta = json.load(f)
        except ValueError:
            meta = {}
    stdout = ""
    if os.path.exists(f_ra):
        with open(f_ra, "rb") as f:
            stdout = chuan_hoa_stdout(
                f.read(MAX_STDOUT).decode("utf-8", "replace")
            )
    bi_cat = bool(meta.get("truncated"))

    if het_gio:
        kq = {"status": "TIME_LIMIT", "time_ms": ms, "stdout": stdout, "error": None, "error_line": None}
    elif "loi_debugger" in meta or p.returncode != 0 and not meta:
        chi_tiet = meta.get("loi_debugger") or (p.stderr.decode("utf-8", "replace")[-500:] if p else "")
        kq = {"status": "RUNTIME_ERROR", "time_ms": ms, "stdout": stdout,
              "error": "Không chạy từng dòng được: " + chi_tiet.strip(), "error_line": None}
    elif meta.get("tin_hieu"):
        ten = meta["tin_hieu"]
        so = getattr(signal, ten, None)
        loi = TIN_HIEU.get(so, f"Chương trình bị dừng bởi tín hiệu {ten}")
        if ten in ("SIGXCPU", "SIGKILL"):
            kq = {"status": "TIME_LIMIT", "time_ms": ms, "stdout": stdout, "error": None, "error_line": None}
        else:
            kq = {"status": "RUNTIME_ERROR", "time_ms": ms, "stdout": stdout, "error": loi,
                  "error_line": meta.get("dong")}
    elif la_loi_het_cpu(meta.get("ma", 0)):
        # JVM bài làm chết vì giới hạn CPU kế thừa từ debugger: quá giờ
        kq = {"status": "TIME_LIMIT", "time_ms": ms, "stdout": stdout, "error": None, "error_line": None}
    elif meta.get("ma", 0) != 0:
        loi, dong = loi_chay(tm.ngon_ngu, meta["ma"], meta.get("stderr", ""))
        kq = {"status": "RUNTIME_ERROR", "time_ms": ms, "stdout": stdout, "error": loi,
              "error_line": dong or meta.get("dong")}
    else:
        kq = {"status": "OK", "time_ms": ms, "stdout": stdout, "error": None, "error_line": None}
    return su_kien, kq, bi_cat


def _lop_jdi():
    """Thư mục có JdiBuoc.class: image build sẵn cạnh file .java; chế độ không sandbox thì biên dịch một lần."""
    if os.path.exists(os.path.join(THU_MUC_BUOC, "JdiBuoc.class")):
        return THU_MUC_BUOC
    dich = os.path.join(tempfile.gettempdir(), "labcast-jdi")
    source = os.path.join(THU_MUC_BUOC, "JdiBuoc.java")
    compiled = os.path.join(dich, "JdiBuoc.class")
    # Nếu Java debugger thay đổi (ví dụ có mốc stdout theo bước), build lại class cache.
    if not os.path.exists(compiled) or os.path.getmtime(compiled) < os.path.getmtime(source):
        os.makedirs(dich, exist_ok=True)
        javac = os.path.join(os.path.dirname(_java()), "javac") if os.path.isabs(_java()) else "javac"
        subprocess.run([javac, "--release", "17", "-encoding", "UTF-8", "-d", dich,
                        source], check=True, capture_output=True, timeout=60)
    return dich


# ---- run.py: một lượt Chạy thử ---------------------------------------------------------------------------


def chay_job(job, out):
    """Chạy thử (run.py) cho C/C++/Java: in trace (chỉ khi chạy từng dòng) rồi đúng một dòng kết quả."""
    ngon_ngu = job["ngon_ngu"]
    code = job.get("code", "")
    du_lieu = job.get("input", "")
    gioi_han_ms = int(job.get("time_limit_ms", 1000))
    tung_dong = bool(job.get("trace")) and bool(job.get("line_trace"))
    ten = "__trace_table__"
    if job.get("trace"):
        out.write(json.dumps({"t": "hdr", "lesson": job.get("lesson", ""), "views": [], "source": code,
                            "lang": ngon_ngu}, ensure_ascii=False) + "\n")
    with ThuMuc(ngon_ngu, code) as tm:
        ok_bd, loi_bd, dong_bd = tm.bien_dich(debug=tung_dong)
        if not ok_bd:
            if job.get("trace"):
                out.write(json.dumps({"t": "end", "truncated": False}) + "\n")
            _ket_qua(out, {"ok": False, "stdout": "", "error": loi_bd, "time_ms": 0,
                           "error_line": dong_bd, "error_kind": "bien-dich"})
            return
        bi_cat = False
        if tung_dong:
            su_kien, kq, bi_cat = chay_tung_dong(tm, du_lieu, gioi_han_ms)
            out.write(json.dumps({"t": "snap", "n": 0, "state": {ten: {
                "kieu": "code-bien", "src": code, "dong": 0, "ham": "", "bien": []}}}, ensure_ascii=False) + "\n")
            for e in su_kien:
                e["v"] = ten
                out.write(json.dumps(e, ensure_ascii=False) + "\n")
        else:
            kq = chay(tm, du_lieu, gioi_han_ms)
        if job.get("trace"):
            out.write(json.dumps({"t": "end", "truncated": bi_cat}) + "\n")
        ok = kq["status"] == "OK"
        loi = kq["error"]
        if kq["status"] == "TIME_LIMIT":
            loi = f"Quá thời gian {gio_cua(ngon_ngu, gioi_han_ms)} ms"
        _ket_qua(out, {"ok": ok, "stdout": kq["stdout"], "error": loi, "time_ms": kq["time_ms"],
                       "truncated": bi_cat, "stdout_truncated": kq.get("stdout_truncated", False),
                       "error_line": kq.get("error_line"), "error_kind": None if ok else "chay"})


def _ket_qua(out, kq):
    kq = {"t": "result", "ok": False, "stdout": "", "error": None, "truncated": False, "stdout_truncated": False,
          "time_ms": 0, **kq}
    if kq.get("error_line") is None:
        kq.pop("error_line", None)
        kq.pop("error_kind", None)
    out.write(json.dumps(kq, ensure_ascii=False) + "\n")
    out.flush()


if __name__ == "__main__":
    sys.exit("Dùng qua run.py hoặc cham.py")