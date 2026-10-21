"""Sinh dữ liệu ẩn cho CSDL truong-hoc (spec §7.6). Chép file này vào thư mục bài dùng CSDL này.

    python sinh_du_lieu.py <seed>      # in ra các câu INSERT, chạy sau schema.sql

Cùng seed thì cùng dữ liệu. Server chấm câu của học viên trên dữ liệu mẫu và 3 database ẩn (seed 1, 2, 3):
- seed 1: nhiều NULL (sinh viên chưa xếp lớp, chưa có quê, vắng thi), có ngày sinh đúng mốc 2005-01-01;
- seed 2: nhiều dòng trùng (trùng họ tên, trùng quê, cùng điểm, một lần thi ghi hai lần);
- seed 3: một bảng rỗng (ket_qua) và một lớp không có sinh viên.
"""

import random
import sys

HO = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô"]
DEM = ["Văn", "Thị", "Minh", "Thu", "Quốc", "Ngọc", "Đức", "Thanh"]
TEN = ["An", "Bình", "Cường", "Dung", "Em", "Giang", "Huy", "Lan", "Minh", "Ngọc", "Phúc", "Quân"]
QUE = ["Hà Nội", "Nam Định", "Hải Phòng", "Thanh Hoá", "Nghệ An", "Bắc Ninh"]
KHOA = ["CNTT", "ATTT", "Kinh tế"]


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, str):
        return "'" + v.replace("'", "''") + "'"
    return str(v)


def chen(bang, dong):
    return f"INSERT INTO {bang} VALUES ({', '.join(q(v) for v in dong)});"


def sinh(seed):
    r = random.Random(seed)
    out = []
    n_gv = 4
    for i in range(1, n_gv + 1):
        out.append(chen("giang_vien", [i, f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}", r.choice(KHOA),
                                       r.choice([16, 18, 18, 21, 24]) * 1_000_000]))
    n_lop = r.randint(3, 5)
    for i in range(1, n_lop + 1):
        gvcn = None if (seed == 1 and i % 2 == 0) else r.randint(1, n_gv)
        out.append(chen("lop", [i, f"D22CQ{r.choice(['CN', 'AT', 'KT'])}{i:02d}", r.choice(KHOA), gvcn]))
    # seed 3: lớp cuối không có sinh viên
    lop_co_sv = n_lop - 1 if seed == 3 else n_lop

    n_sv = r.randint(14, 24)
    ten_trung = f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}"
    for i in range(1, n_sv + 1):
        ho_ten = ten_trung if (seed == 2 and i % 4 == 0) else f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}"
        if seed == 1 and i in (2, 5):
            ngay = "2005-01-01"  # đúng mốc: phân biệt >= với >
        else:
            ngay = f"{r.randint(2003, 2006)}-{r.randint(1, 12):02d}-{r.randint(1, 28):02d}"
        que = r.choice(QUE)
        if seed == 1 and i % 5 == 0:
            que = None
        if seed == 2:
            que = "Hà Nội" if i % 2 else que
        lop = r.randint(1, lop_co_sv)
        if seed == 1 and i % 3 == 0:
            lop = None
        out.append(chen("sinh_vien", [i, ho_ten, r.choice(["Nam", "Nữ"]), ngay, que, lop]))

    for i, (ma, ten, tc) in enumerate(
        [("INT1433", "Lập trình mạng", 3), ("INT1313", "Cơ sở dữ liệu", 3), ("BAS1203", "Giải tích 1", 3),
         ("INT1155", "Tin học cơ sở 2", 2)], start=1):
        out.append(chen("mon_hoc", [i, ma, ten, tc]))

    if seed != 3:
        for sv in range(1, n_sv + 1):
            for mon in range(1, 3):
                if r.random() < 0.2:
                    continue
                diem = r.choice([4.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 10.0, 3.5, 2.0])
                if seed == 1 and r.random() < 0.15:
                    diem = None
                out.append(chen("ket_qua", [sv, mon, 1, diem]))
                if seed == 2 and r.random() < 0.15:
                    out.append(chen("ket_qua", [sv, mon, 1, diem]))  # ghi trùng một lần thi
                if diem is not None and diem < 5 and r.random() < 0.7:
                    out.append(chen("ket_qua", [sv, mon, 2, r.choice([5.0, 6.0, 7.5, 3.0])]))
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdout.write(sinh(int(sys.argv[1])))
