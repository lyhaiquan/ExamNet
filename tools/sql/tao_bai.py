"""Sinh thư mục bài SQL trong content/sql/ từ danh sách bài trong tools/sql/bai/<chương>.py.

    python tools/sql/tao_bai.py              # sinh mọi bài, rồi kiểm bằng sqlite3
    python tools/sql/tao_bai.py ket-noi      # chỉ một chương
    python tools/sql/tao_bai.py --kiem       # chỉ kiểm, không ghi file

Mỗi bài là một dict:

    id, ten, loai (giang-giai | bai-tap | luyen-tap), csdl (truong-hoc | ban-hang | nhan-su), de (markdown),
    dap_an (một câu SQL), bang (các bảng in trong đề), tuỳ chọn: bac, thu_tu (True: chấm thứ tự dòng),
    loai_cau (select | dml), cot (tên cột kết quả, mặc định lấy từ đáp án), sai (các câu sai hay gặp: phải bị
    chấm sai), them_schema (CREATE TABLE riêng của bài), them_du_lieu (INSERT thêm vào dữ liệu mẫu).

Bài sinh ra tự đủ file như bài viết tay (spec §7.1): chép schema.sql, du_lieu_mau.sql, sinh_du_lieu.py của CSDL
dùng chung, thêm lesson.yaml, de.md (bảng dữ liệu mẫu in từ du_lieu_mau.sql), dap_an.sql, khung.sql, sai.sql.
`sai.sql` chỉ server đọc (SqlLabTest kiểm mỗi câu trong đó bị chấm sai); app không nhận file này.

Kiểm (giống SqlLab chấm): đáp án chạy được trên dữ liệu mẫu và 3 database ẩn, ra ít nhất một dòng trên dữ liệu
mẫu; mỗi câu sai cho kết quả khác đáp án ở ít nhất một database.
"""

import importlib.util
import os
import shutil
import sqlite3
import subprocess
import sys

GOC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SQL = os.path.join(GOC, "content", "sql")
DS_BAI = os.path.join(os.path.dirname(__file__), "bai")
CHUONG = ["co-ban", "gom-nhom", "null-case", "dml", "ket-noi", "truy-van-con", "tap-hop", "tong-hop"]

CHAN = (
    "Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải "
    "đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy."
)

# Ghi chú cột NULL / miền giá trị, in cạnh tên bảng trong đề
GHI_CHU = {
    "sinh_vien": "`lop_id` NULL: chưa xếp lớp; `que_quan` có thể NULL",
    "lop": "`gvcn_id` NULL: chưa có chủ nhiệm",
    "ket_qua": "`diem` NULL: vắng thi; `lan_thi` 1 là thi lần đầu, 2 là thi lại",
    "khach_hang": "`thanh_pho` NULL: chưa ghi",
    "danh_muc": "`cha_id` NULL: danh mục gốc",
    "don_hang": "`khach_hang_id` NULL: khách vãng lai; `trang_thai` là `moi`, `dang-giao`, `da-giao` hoặc `huy`",
    "chi_tiet_don": "thành tiền một dòng = `so_luong * don_gia`",
    "nhan_vien": "`phong_id` NULL: chưa xếp phòng; `quan_ly_id` NULL: không có người quản lý",
    "du_an": "`phong_id` NULL: dự án chung, chưa giao phòng nào",
}

KHUNG = {
    "select": "-- Viết MỘT câu SELECT. Bấm Chạy thử để xem từng bước thực thi.\nSELECT \n",
    "INSERT": "-- Viết MỘT câu INSERT. Bấm Chạy thử để xem bảng trước và sau khi đổi.\nINSERT INTO \n",
    "UPDATE": "-- Viết MỘT câu UPDATE. Bấm Chạy thử để xem bảng trước và sau khi đổi.\nUPDATE \n",
    "DELETE": "-- Viết MỘT câu DELETE. Bấm Chạy thử để xem bảng trước và sau khi đổi.\nDELETE FROM \n",
}

BAC = {"giang-giai": ["xem", "du-doan", "code"], "bai-tap": ["code"], "luyen-tap": ["code"]}


def doc(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def ghi(p, s):
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        f.write(s)


def nap_chuong(ten):
    """Bài của một chương: tools/sql/bai/<chương>.py, cộng các file <chương>--<phần>.py nếu chương dài."""
    bai = []
    for f in sorted(os.listdir(DS_BAI)):
        if f == ten + ".py" or (f.startswith(ten + "--") and f.endswith(".py")):
            spec = importlib.util.spec_from_file_location(f[:-3].replace("-", "_"), os.path.join(DS_BAI, f))
            m = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(m)
            bai += m.BAI
    return bai


# ---------- dữ liệu ----------


def schema(b):
    s = doc(os.path.join(SQL, "_csdl", b["csdl"], "schema.sql"))
    if b.get("them_schema"):
        s += "\n-- Bảng riêng của bài này\n" + b["them_schema"].strip() + "\n"
    return s


def du_lieu_mau(b):
    s = doc(os.path.join(SQL, "_csdl", b["csdl"], "du_lieu_mau.sql"))
    if b.get("them_du_lieu"):
        s += "\n-- Thêm riêng cho bài này\n" + b["them_du_lieu"].strip() + "\n"
    return s


_AN = {}


def du_lieu_an(csdl):
    if csdl not in _AN:
        p = os.path.join(SQL, "_csdl", csdl, "sinh_du_lieu.py")
        _AN[csdl] = [
            subprocess.run([sys.executable, p, str(seed)], capture_output=True, check=True).stdout.decode("utf-8")
            for seed in (1, 2, 3)
        ]
    return _AN[csdl]


def mo(schema_sql, data_sql):
    c = sqlite3.connect(":memory:")
    c.executescript(schema_sql)
    c.executescript(data_sql)
    return c


def bo_cham_phay(sql):
    return sql.strip().rstrip(";").strip()


def bang_md(c, bang):
    cur = c.execute(f"SELECT * FROM {bang}")
    cot = [d[0] for d in cur.description]
    dong = cur.fetchall()
    out = [f"| {' | '.join(cot)} |", "| " + " | ".join("---" for _ in cot) + " |"]
    for d in dong:
        out.append("| " + " | ".join("NULL" if v is None else str(v) for v in d) + " |")
    if not dong:
        out.append("| " + " | ".join("" for _ in cot) + " |")
    return cot, "\n".join(out)


# ---------- so kết quả như ResultComparer ----------


def _khoa(v):
    if v is None:
        return (0, 0)
    if isinstance(v, (int, float)):
        return (1, float(v))
    return (2, str(v))


def giong_gia_tri(a, b):
    if a is None or b is None:
        return a is b
    if isinstance(a, (int, float)) and isinstance(b, (int, float)):
        return abs(a - b) <= 1e-6 * max(1.0, abs(a), abs(b))
    if isinstance(a, (int, float)) or isinstance(b, (int, float)):
        return False
    return str(a) == str(b)


def giong(x, y, co_thu_tu):
    (cx, rx), (cy, ry) = x, y
    if len(cx) != len(cy) or len(rx) != len(ry):
        return False
    if not co_thu_tu:
        rx = sorted(rx, key=lambda r: [_khoa(v) for v in r])
        ry = sorted(ry, key=lambda r: [_khoa(v) for v in r])
    return all(len(a) == len(b) and all(giong_gia_tri(u, v) for u, v in zip(a, b)) for a, b in zip(rx, ry))


def chay(b, data_sql, cau):
    c = mo(schema(b), data_sql)
    if b.get("loai_cau") == "dml":
        c.execute(bo_cham_phay(cau))
        tat_ca = []
        for (t,) in c.execute("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name").fetchall():
            cur = c.execute(f"SELECT * FROM {t}")
            tat_ca.append((t, [d[0] for d in cur.description], cur.fetchall()))
        return tat_ca
    cur = c.execute(bo_cham_phay(cau))
    return [d[0] for d in cur.description], cur.fetchall()


def cung_trang_thai(a, b):
    return len(a) == len(b) and all(
        ta == tb and giong((ca, ra), (cb, rb), False) for (ta, ca, ra), (tb, cb, rb) in zip(a, b)
    )


def kiem(b):
    """Danh sách lỗi của một bài (rỗng: đạt)."""
    loi = []
    bo = [du_lieu_mau(b)] + du_lieu_an(b["csdl"])
    dml = b.get("loai_cau") == "dml"
    try:
        dung = [chay(b, d, b["dap_an"]) for d in bo]
    except sqlite3.Error as e:
        return [f"đáp án lỗi: {e}"]
    if not dml and not dung[0][1]:
        loi.append("đáp án ra 0 dòng trên dữ liệu mẫu")
    if not dml and b.get("cot") and len(b["cot"]) != len(dung[0][0]):
        loi.append(f"cot {b['cot']} khác số cột đáp án {dung[0][0]}")
    for s in b.get("sai", []):
        bat = False
        for d, kq in zip(bo, dung):
            try:
                r = chay(b, d, s)
            except sqlite3.Error:
                bat = True
                break
            if not (cung_trang_thai(r, kq) if dml else giong(r, kq, b.get("thu_tu", False))):
                bat = True
                break
        if not bat:
            loi.append(f"câu sai vẫn được chấm đúng: {s}")
    return loi


# ---------- ghi bài ----------


def chu_dong(cau):
    return bo_cham_phay(cau).lstrip("( \n").split(None, 1)[0].upper()


def lesson_yaml(b, chuong):
    dml = b.get("loai_cau") == "dml"
    bac = b.get("bac") or BAC[b["loai"]]
    thu_tu = "co                   # co: so kết quả theo đúng thứ tự dòng" if b.get("thu_tu") else (
        "khong                   # khong: so kết quả như đa tập, không xét thứ tự dòng")
    return (
        f"id: sql.{chuong}.{b['id']}\n"
        "mon: sql\n"
        f"chuong: {chuong}\n"
        f"ten: {b['ten']}\n"
        f"loai: {b['loai']}            # giang-giai | bai-tap | luyen-tap\n"
        f"bac: [{', '.join(bac)}]\n"
        "khung_nhin:                     # câu SQL vẽ bằng bộ vẽ bảng\n"
        "  - bien: sql\n"
        "    kieu: bang-sql\n"
        f"loai_cau: {'dml' if dml else 'select'}                # select | dml\n"
        f"thu_tu: {thu_tu}\n"
        "gioi_han: { thoi_gian_ms: 2000 }\n"
        "mo_khoa: []\n"
    )


def de_md(b):
    c = mo(schema(b), du_lieu_mau(b))
    parts = [f"# {b['ten']}", b["de"].strip()]
    if b.get("loai_cau") == "dml":
        parts.append("Chấm bằng cách so **toàn bộ** database sau khi chạy câu của em với database sau khi chạy đáp án.")
    else:
        cot = b.get("cot") or [d[0] for d in c.execute(bo_cham_phay(b["dap_an"])).description]
        thu_tu = "**Thứ tự dòng được chấm.**" if b.get("thu_tu") else "Thứ tự dòng không quan trọng."
        parts.append(f"Kết quả có {len(cot)} cột: {', '.join(cot)}. {thu_tu}")
    parts.append("## Bảng dùng trong bài")
    for t in b["bang"]:
        cot, md = bang_md(c, t)
        dau = f"`{t}({', '.join(cot)})`"
        if t in GHI_CHU:
            dau += " — " + GHI_CHU[t]
        if t in b.get("ghi_chu_bang", {}):
            dau += " — " + b["ghi_chu_bang"][t]
        parts.append(dau + "\n\n" + md)
    parts.append(CHAN)
    return "\n\n".join(parts) + "\n"


def tao(b, chuong):
    d = os.path.join(SQL, chuong, b["id"])
    os.makedirs(d, exist_ok=True)
    nguon = os.path.join(SQL, "_csdl", b["csdl"])
    shutil.copyfile(os.path.join(nguon, "sinh_du_lieu.py"), os.path.join(d, "sinh_du_lieu.py"))
    ghi(os.path.join(d, "schema.sql"), schema(b))
    ghi(os.path.join(d, "du_lieu_mau.sql"), du_lieu_mau(b))
    ghi(os.path.join(d, "lesson.yaml"), lesson_yaml(b, chuong))
    ghi(os.path.join(d, "de.md"), de_md(b))
    ghi(os.path.join(d, "dap_an.sql"), b["dap_an"].strip() + "\n")
    loai = "select" if b.get("loai_cau") != "dml" else chu_dong(b["dap_an"])
    ghi(os.path.join(d, "khung.sql"), KHUNG[loai])
    sai = os.path.join(d, "sai.sql")
    if b.get("sai"):
        ghi(sai, "-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.\n\n"
            + "\n\n".join(bo_cham_phay(s) + ";" for s in b["sai"]) + "\n")
    elif os.path.exists(sai):
        os.remove(sai)


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    chi_kiem = "--kiem" in sys.argv
    chuong = args or CHUONG
    so_loi = 0
    so_bai = 0
    for ch in chuong:
        for b in nap_chuong(ch):
            so_bai += 1
            loi = kiem(b)
            if loi:
                so_loi += 1
                for e in loi:
                    print(f"✘ sql.{ch}.{b['id']}: {e}")
                continue
            if not chi_kiem:
                tao(b, ch)
    print(f"{so_bai} bài, {so_loi} bài lỗi" + ("" if chi_kiem else " (bài lỗi không được ghi)"))
    sys.exit(1 if so_loi else 0)


if __name__ == "__main__":
    main()
