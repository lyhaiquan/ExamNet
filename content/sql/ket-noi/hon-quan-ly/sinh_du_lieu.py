"""Sinh dữ liệu ẩn cho CSDL nhan-su (spec §7.6). Chép file này vào thư mục bài dùng CSDL này.

    python sinh_du_lieu.py <seed>      # in ra các câu INSERT, chạy sau schema.sql

Cùng seed thì cùng dữ liệu. Server chấm câu của học viên trên dữ liệu mẫu và 3 database ẩn (seed 1, 2, 3):
- seed 1: nhiều NULL (nhân viên chưa xếp phòng, nhiều người không có quản lý, dự án chưa giao phòng);
- seed 2: nhiều dòng trùng (trùng họ tên, nhiều người cùng lương, đúng bằng lương trung bình);
- seed 3: một bảng rỗng (phan_cong) và một phòng không có nhân viên.
Phòng "Kỹ thuật" luôn có, nhưng số hiệu thay đổi theo seed: câu đúng phải tìm phòng theo tên.
"""

import random
import sys

HO = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô"]
DEM = ["Văn", "Thị", "Minh", "Thu", "Quốc", "Ngọc", "Đức", "Thanh"]
TEN = ["Hùng", "Mai", "Nam", "Oanh", "Phúc", "Quỳnh", "Sơn", "Thảo", "Tuấn", "Uyên", "Vân", "Xuân"]
PHONG = ["Kinh doanh", "Nhân sự", "Kỹ thuật", "Pháp chế", "Tài chính"]


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, str):
        return "'" + v.replace("'", "''") + "'"
    return str(v)


def chen(bang, dong):
    return f"INSERT INTO {bang} VALUES ({', '.join(q(v) for v in dong)});"


def sinh(seed):
    r = random.Random(f"nhan-su-{seed}")
    out = []
    phong = PHONG[:]
    r.shuffle(phong)
    n_pb = len(phong)
    for i, ten in enumerate(phong, start=1):
        out.append(chen("phong_ban", [i, ten, r.randint(1, 50) * 10_000_000]))
    pb_co_nv = n_pb - 1 if seed == 3 else n_pb  # seed 3: phòng cuối không có nhân viên

    n_nv = r.randint(14, 24)
    ten_trung = f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}"
    for i in range(1, n_nv + 1):
        ho_ten = ten_trung if (seed == 2 and i % 4 == 0) else f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}"
        pb = r.randint(1, pb_co_nv)
        if seed == 1 and i % 4 == 0:
            pb = None
        ql = None if i == 1 or (seed == 1 and i % 5 == 0) else r.randint(1, i - 1)
        luong = r.choice([12, 15, 18, 20, 25]) * 1_000_000 if seed == 2 else r.randint(10, 45) * 1_000_000
        out.append(chen("nhan_vien", [i, ho_ten, pb, ql, luong,
                                      f"{r.randint(2012, 2024)}-{r.randint(1, 12):02d}-{r.randint(1, 28):02d}"]))

    n_da = r.randint(4, 7)
    for i in range(1, n_da + 1):
        pb = None if (seed == 1 and i % 2 == 0) else r.randint(1, n_pb)
        out.append(chen("du_an", [i, f"Dự án {i}", pb, r.randint(1, 90) * 10_000_000]))

    if seed != 3:
        for nv in range(1, n_nv + 1):
            for da in r.sample(range(1, n_da + 1), r.randint(0, 2)):
                out.append(chen("phan_cong", [nv, da, r.randint(1, 20) * 10]))
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdout.write(sinh(int(sys.argv[1])))
