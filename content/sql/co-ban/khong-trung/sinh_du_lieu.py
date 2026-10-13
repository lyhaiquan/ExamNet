"""Sinh dữ liệu ẩn cho CSDL ban-hang (spec §7.6). Chép file này vào thư mục bài dùng CSDL này.

    python sinh_du_lieu.py <seed>      # in ra các câu INSERT, chạy sau schema.sql

Cùng seed thì cùng dữ liệu. Server chấm câu của học viên trên dữ liệu mẫu và 3 database ẩn (seed 1, 2, 3):
- seed 1: nhiều NULL (khách chưa ghi thành phố, đơn của khách vãng lai, danh mục gốc);
- seed 2: nhiều dòng trùng (trùng họ tên, trùng thành phố, nhiều sản phẩm cùng giá, khách đặt nhiều đơn);
- seed 3: hai bảng rỗng (don_hang, chi_tiet_don): không ai đặt đơn nào.
"""

import random
import sys

HO = ["Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ", "Ngô"]
DEM = ["Văn", "Thị", "Minh", "Thu", "Quốc", "Ngọc", "Đức", "Thanh"]
TEN = ["An", "Bình", "Cường", "Dung", "Em", "Giang", "Huy", "Lan", "Minh", "Ngọc", "Phúc", "Quân"]
PHO = ["Hà Nội", "Hồ Chí Minh", "Đà Nẵng", "Hải Phòng", "Cần Thơ", "Huế"]
TRANG_THAI = ["moi", "dang-giao", "da-giao", "huy"]
SAN_PHAM = ["Điện thoại A", "Điện thoại B", "Laptop C", "Laptop D", "Tai nghe E", "Sạc F", "Sách G", "Sách H",
            "Chuột I", "Bàn phím K", "Màn hình L", "Loa M"]


def q(v):
    if v is None:
        return "NULL"
    if isinstance(v, str):
        return "'" + v.replace("'", "''") + "'"
    return str(v)


def chen(bang, dong):
    return f"INSERT INTO {bang} VALUES ({', '.join(q(v) for v in dong)});"


def sinh(seed):
    r = random.Random(f"ban-hang-{seed}")
    out = []
    n_kh = r.randint(12, 20)
    ten_trung = f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}"
    for i in range(1, n_kh + 1):
        ho_ten = ten_trung if (seed == 2 and i % 4 == 0) else f"{r.choice(HO)} {r.choice(DEM)} {r.choice(TEN)}"
        pho = r.choice(PHO)
        if seed == 1 and i % 3 == 0:
            pho = None
        if seed == 2:
            pho = "Hà Nội" if i % 2 else pho
        out.append(chen("khach_hang", [i, ho_ten, pho, f"2024-{r.randint(1, 12):02d}-{r.randint(1, 28):02d}"]))

    dm = [(1, "Điện tử", None), (2, "Điện thoại", 1), (3, "Máy tính", 1), (4, "Sách", None), (5, "Phụ kiện", 1)]
    for d in dm:
        out.append(chen("danh_muc", list(d)))

    n_sp = r.randint(8, 12)
    for i in range(1, n_sp + 1):
        gia = r.choice([150000, 600000, 5000000, 16000000, 20000000]) if seed == 2 else r.randint(1, 300) * 100000
        out.append(chen("san_pham", [i, SAN_PHAM[i - 1], r.randint(1, 5), gia, r.randint(0, 50)]))

    if seed != 3:
        n_dh = r.randint(12, 25)
        for i in range(1, n_dh + 1):
            kh = r.randint(1, n_kh // 2) if seed == 2 else r.randint(1, n_kh)  # nửa sau không đặt đơn nào
            if seed == 1 and i % 4 == 0:
                kh = None
            out.append(chen("don_hang", [i, kh, f"2024-{r.randint(1, 12):02d}-{r.randint(1, 28):02d}",
                                         r.choice(TRANG_THAI)]))
            for _ in range(r.randint(1, 3)):
                sp = r.randint(1, n_sp)
                out.append(chen("chi_tiet_don", [i, sp, r.randint(1, 4), r.randint(1, 300) * 100000]))
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    sys.stdout.write(sinh(int(sys.argv[1])))
