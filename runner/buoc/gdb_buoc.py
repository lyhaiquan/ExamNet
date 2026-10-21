"""Chạy từng dòng một chương trình C/C++ dưới gdb, ghi sự kiện `line`/`vars` (spec đa ngôn ngữ §5).

Chạy bằng API Python của gdb, không phải bằng python thường:

    gdb -q -nx -batch -x gdb_buoc.py --args ./main

Đọc các biến môi trường LC_SRC (tên file học viên, ví dụ main.cpp), LC_IN / LC_OUT (stdin / stdout của
chương trình), LC_TRACE (file JSONL ghi sự kiện), LC_META (JSON kết quả: mã thoát, tín hiệu, bị cắt hay không),
LC_MAX (số dòng tối đa được ghi).

Không dùng `step`: một vòng lặp nằm gọn trên một dòng (`while (true) { x++; }`) nhảy về chính dòng đó, `step`
chạy từng lệnh máy mãi không dừng. Thay vào đó đặt breakpoint ở mọi dòng có code của file học viên rồi
`continue`: dừng đúng mỗi lần chương trình tới một dòng (kể cả quay lại dòng cũ), không bao giờ dừng trong
thư viện chuẩn, và nhanh vì không chạy từng lệnh máy.
"""

import json
import os
import re

import gdb  # noqa: I001 — chỉ có khi chạy trong gdb

SRC = os.environ["LC_SRC"]
MAX = int(os.environ.get("LC_MAX", "800"))
MAX_GIA_TRI = 120

# ghi theo dòng: debugger bị giết vì quá giờ thì các bước đã ghi vẫn còn
trace = open(os.environ["LC_TRACE"], "w", encoding="utf-8", buffering=1)
meta = {"ma": 0, "truncated": False}
tin_hieu = [None]

# "std::vector of length 3, capacity 4 = {1, 2, 3}" → "{1, 2, 3}"; "std::map with 2 elements = {...}" → "{...}"
_VO_BOC = re.compile(r"^std::[\w:]+(?:<.*?>)? (?:of length \d+(?:, capacity \d+)?|with \d+ elements?)(?: = )?")


try:
    with open(SRC, encoding="utf-8", errors="replace") as _f:
        DONG_CODE = _f.read().splitlines()
except OSError:
    DONG_CODE = []


def _chi_dong_ngoac(dong):
    return 0 < dong <= len(DONG_CODE) and DONG_CODE[dong - 1].strip() in ("}", "};")


def ghi(su_kien):
    trace.write(json.dumps(su_kien, ensure_ascii=False) + "\n")


def lenh(c):
    return gdb.execute(c, to_string=True)


def hien(gia_tri):
    t = gia_tri.type.strip_typedefs()
    if t.code == gdb.TYPE_CODE_PTR and t.target().strip_typedefs().code != gdb.TYPE_CODE_INT or (
        t.code == gdb.TYPE_CODE_PTR and t.target().strip_typedefs().sizeof != 1
    ):
        # con trỏ (trừ char*): in địa chỉ, NULL cho rõ; "set print address off" sẽ in thành chuỗi rỗng
        try:
            so = int(gia_tri)
        except (gdb.error, ValueError):
            return "?"
        return "NULL" if so == 0 else f"{so:#x}"
    if t.code == gdb.TYPE_CODE_ARRAY and t.target().strip_typedefs().code == gdb.TYPE_CODE_INT and (
        t.target().strip_typedefs().sizeof == 1
    ):
        # mảng char: chuỗi tới ký tự '\0' đầu tiên, phần sau là rác chưa ghi
        try:
            chuoi = gia_tri.string(errors="replace")
            return json.dumps(chuoi, ensure_ascii=False)[: MAX_GIA_TRI]
        except (gdb.error, UnicodeDecodeError):
            pass
    try:
        s = gia_tri.format_string(max_elements=20, repeat_threshold=10, address=False, symbols=False,
                                  styling=False, pretty_structs=False, pretty_arrays=False)
    except (gdb.error, TypeError):
        s = str(gia_tri)
    s = " ".join(s.split())
    m = _VO_BOC.match(s)
    if m:
        s = s[m.end():] or "{}"
    return s if len(s) <= MAX_GIA_TRI else s[: MAX_GIA_TRI - 1] + "…"


def doc_bien(khung, dong):
    """Biến cục bộ và tham số của hàm đang chạy, đã khai báo trước dòng sắp chạy."""
    co = []
    da_co = set()
    try:
        khoi = khung.block()
    except RuntimeError:
        return []
    while khoi is not None:
        for sym in khoi:
            if not (sym.is_variable or sym.is_argument) or sym.name in da_co:
                continue
            # biến khai báo ở dòng sắp chạy (hay sau) chưa có giá trị thật: chưa hiện
            if not sym.is_argument and sym.line >= dong:
                continue
            try:
                v = hien(sym.value(khung))
            except (gdb.error, RuntimeError, gdb.MemoryError):
                continue
            da_co.add(sym.name)
            co.append((0 if sym.is_argument else 1, sym.line, sym.name, v))
        if khoi.function is not None:
            break
        khoi = khoi.superblock
    co.sort(key=lambda x: (x[0], x[1]))
    return [[ten, v] for _, _, ten, v in co]


def khi_dung(su_kien):
    if isinstance(su_kien, gdb.SignalEvent) and su_kien.stop_signal not in ("SIGTRAP", "SIGINT"):
        tin_hieu[0] = su_kien.stop_signal


def khi_thoat(su_kien):
    if hasattr(su_kien, "exit_code"):
        meta["ma"] = su_kien.exit_code


breakpoints = []


def dat_breakpoint():
    """Breakpoint ở mọi địa chỉ đầu câu lệnh của file học viên, lấy từ bảng dòng của trình biên dịch.

    Một dòng có thể có nhiều địa chỉ (dòng `for`: khởi tạo, kiểm điều kiện, tăng biến) nên vòng lặp quay lại
    dòng đó lần nào cũng dừng được.
    """
    try:
        symtab = gdb.decode_line(f"{SRC}:1")[1][0].symtab
    except (gdb.error, IndexError, TypeError):
        symtab = None
    if symtab is None:
        raise RuntimeError("không đọc được bảng dòng của " + SRC + " (thiếu -g?)")
    dia_chi = set()
    for muc in symtab.linetable():
        if muc.line > 0 and getattr(muc, "is_stmt", True):
            dia_chi.add(muc.pc)
    for pc in sorted(dia_chi):
        try:
            breakpoints.append(gdb.Breakpoint(f"*{pc:#x}", internal=True))
        except (gdb.error, RuntimeError):
            pass


def xoa_breakpoint():
    for bp in breakpoints:
        try:
            bp.delete()
        except RuntimeError:
            pass
    breakpoints.clear()


def dang_chay():
    inf = gdb.selected_inferior()
    return inf is not None and inf.pid != 0


def main():
    for c in ("set pagination off", "set confirm off", "set width 0", "set height 0",
              "set print frame-arguments none", "set print address off", "set print symbol off",
              "set print object off", "set print static-members off", "set debuginfod enabled off",
              "set breakpoint pending off", "set disable-randomization off"):
        try:
            lenh(c)
        except gdb.error:
            pass
    gdb.events.stop.connect(khi_dung)
    gdb.events.exited.connect(khi_thoat)
    # đặt breakpoint theo bảng dòng sau khi chương trình đã nạp: file chạy PIE chỉ có địa chỉ thật lúc này
    lenh("tbreak main")
    lenh(f"run < {os.environ['LC_IN']} > {os.environ['LC_OUT']}")
    dat_breakpoint()

    dem = 0
    truoc = None
    dong = 0
    khung_truoc = None
    pc_truoc = 0
    while dang_chay() and tin_hieu[0] is None:
        try:
            khung = gdb.selected_frame()
        except gdb.error:
            break
        sal = khung.find_sal()
        if sal.symtab is None or os.path.basename(sal.symtab.filename) != SRC:
            # dừng ngoài file học viên (chỉ xảy ra khi có tín hiệu): chạy tiếp
            try:
                lenh("continue")
            except gdb.error:
                break
            continue
        # dòng chỉ có "}" là lúc hàm đang kết thúc: destructor đã giải phóng vector/string, đọc ra rác
        bien = truoc if _chi_dong_ngoac(sal.line) and truoc is not None else doc_bien(khung, sal.line)
        pc = khung.pc()
        ham = khung.function()
        if ham is not None and pc == int(ham.value().address):
            # vừa vào hàm, chưa qua phần chuẩn bị (prologue): tham số chưa có giá trị. Dừng tiếp ở dòng đầu thân hàm
            try:
                lenh("continue")
            except gdb.error:
                break
            continue
        # cùng dòng, cùng lời gọi hàm, địa chỉ đi tiếp về sau: vẫn là lần ghé dòng này (ví dụ sau khi gọi
        # constructor của vector, hay phần tăng biến của dòng for) — chỉ cập nhật biến. Nhảy ngược về là
        # vòng lặp mới: thêm một bước
        cung_lan = sal.line == dong and khung == khung_truoc and pc > pc_truoc
        if not cung_lan:
            if dem >= MAX:
                meta["truncated"] = True
                break
            dem += 1
            dong = sal.line
            khung_truoc = khung
            ghi({"t": "line", "n": dong})
        pc_truoc = pc
        if bien != truoc:
            ghi({"t": "vars", "f": khung.name() or "", "d": bien})
            truoc = bien
        try:
            lenh("continue")
        except gdb.error:
            break

    trace.flush()  # chạy nốt có thể bị giết vì quá giờ: các bước đã ghi không được mất
    if dang_chay() and tin_hieu[0] is None:
        # hết trần dòng: chương trình chạy nốt không ghi, để vẫn có output. Giới hạn CPU runner đặt cho gdb
        # được chương trình kế thừa, vòng lặp vô tận sẽ chết vì SIGXCPU
        try:
            xoa_breakpoint()
            lenh("continue")
        except gdb.error:
            pass
    if tin_hieu[0] is not None:
        meta["tin_hieu"] = tin_hieu[0]
        meta["dong"] = dong or None
        try:
            lenh("kill")
        except gdb.error:
            pass
    trace.close()
    with open(os.environ["LC_META"], "w", encoding="utf-8") as f:
        json.dump(meta, f)


try:
    main()
except Exception as e:  # noqa: BLE001 — báo về runner thay vì để gdb in traceback
    trace.close()
    with open(os.environ["LC_META"], "w", encoding="utf-8") as f:
        json.dump({"loi_debugger": f"{type(e).__name__}: {e}"}, f)
