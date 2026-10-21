"""sql.gom-nhom — GROUP BY, hàm gộp, HAVING (docs/plans/danh-muc-bai.md)."""

# Thêm kết quả môn 2, 3 vào dữ liệu mẫu (bản gốc chỉ có môn 1): có điểm dưới 4, điểm đúng 4, vắng thi, thi lại
KET_QUA_THEM = """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 3.5),
    (2, 2, 1, 7.0),
    (3, 2, 1, 2.0),
    (3, 2, 2, 6.0),
    (4, 2, 1, 8.0),
    (5, 2, 1, 4.0),
    (6, 2, 1, NULL),
    (1, 3, 1, 3.0),
    (2, 3, 1, 9.0),
    (4, 3, 1, 1.5);
"""

BAI = [
    {
        "id": "so-sp-danh-muc",
        "ten": "Số sản phẩm mỗi danh mục",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "ban-hang",
        "bang": ["san_pham"],
        "de": """
Cho biết **mỗi danh mục** (`danh_muc_id`) có **bao nhiêu sản phẩm**. Chỉ liệt kê danh mục có ít nhất một sản phẩm.

`GROUP BY danh_muc_id` gom các sản phẩm cùng danh mục thành một nhóm; `COUNT(*)` đếm số dòng trong mỗi nhóm.
""",
        "cot": ["danh_muc_id", "so_san_pham"],
        "dap_an": """
SELECT danh_muc_id, COUNT(*) AS so_san_pham
FROM san_pham
GROUP BY danh_muc_id;
""",
        "sai": [
            "SELECT danh_muc_id, COUNT(*) FROM san_pham",
            "SELECT danh_muc_id, COUNT(DISTINCT danh_muc_id) FROM san_pham GROUP BY danh_muc_id",
        ],
    },
    {
        "id": "tong-tien-don",
        "ten": "Tổng tiền từng đơn hàng",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["chi_tiet_don"],
        "de": """
Tính **tổng tiền** của từng đơn hàng: cộng thành tiền mọi dòng chi tiết của đơn đó. Thành tiền một dòng là
`so_luong * don_gia`.

Hàm gộp nhận được cả biểu thức: `SUM(so_luong * don_gia)` nhân từng dòng rồi mới cộng.
""",
        "cot": ["don_hang_id", "tong_tien"],
        "dap_an": """
SELECT don_hang_id, SUM(so_luong * don_gia) AS tong_tien
FROM chi_tiet_don
GROUP BY don_hang_id;
""",
        "sai": [
            "SELECT don_hang_id, SUM(don_gia) FROM chi_tiet_don GROUP BY don_hang_id",
            "SELECT don_hang_id, SUM(so_luong) * SUM(don_gia) FROM chi_tiet_don GROUP BY don_hang_id",
        ],
    },
    {
        "id": "diem-cao-thap",
        "ten": "Điểm cao nhất, thấp nhất của từng môn",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua"],
        "them_du_lieu": KET_QUA_THEM,
        "de": """
Với **từng môn** có người thi, cho biết **điểm cao nhất** và **điểm thấp nhất** (tính mọi lần thi). Lần vắng thi
(`diem` NULL) không tính.

`MAX` và `MIN` tự bỏ qua NULL.
""",
        "cot": ["mon_hoc_id", "cao_nhat", "thap_nhat"],
        "dap_an": """
SELECT mon_hoc_id, MAX(diem) AS cao_nhat, MIN(diem) AS thap_nhat
FROM ket_qua
GROUP BY mon_hoc_id;
""",
        "sai": [
            "SELECT mon_hoc_id, MAX(diem), MIN(diem) FROM ket_qua",
            "SELECT sinh_vien_id, MAX(diem), MIN(diem) FROM ket_qua GROUP BY sinh_vien_id",
        ],
    },
    {
        "id": "lop-dong",
        "ten": "Lớp có từ 3 sinh viên",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "truong-hoc",
        "bang": ["sinh_vien"],
        "de": """
Liệt kê **mã lớp và sĩ số** của các lớp có **từ 3 sinh viên trở lên**. Sinh viên chưa xếp lớp (`lop_id` NULL)
không thuộc lớp nào.

`WHERE` lọc **dòng** trước khi gom nhóm; `HAVING` lọc **nhóm** sau khi gom, nên điều kiện trên `COUNT(*)` phải đặt
ở `HAVING`. Cẩn thận: `GROUP BY lop_id` gom mọi dòng `lop_id` NULL thành một nhóm riêng.
""",
        "cot": ["lop_id", "si_so"],
        "dap_an": """
SELECT lop_id, COUNT(*) AS si_so
FROM sinh_vien
WHERE lop_id IS NOT NULL
GROUP BY lop_id
HAVING COUNT(*) >= 3;
""",
        "sai": [
            "SELECT lop_id, COUNT(*) FROM sinh_vien GROUP BY lop_id HAVING COUNT(*) >= 3",
            "SELECT lop_id, COUNT(*) FROM sinh_vien WHERE lop_id IS NOT NULL GROUP BY lop_id HAVING COUNT(*) > 3",
        ],
    },
    {
        "id": "khach-than-thiet",
        "ten": "Khách có từ 3 đơn trở lên",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang"],
        "de": """
Liệt kê **mã khách và số đơn** của các khách đã đặt **từ 3 đơn trở lên** (tính mọi trạng thái). Đơn của khách vãng
lai (`khach_hang_id` NULL) không thuộc khách nào.
""",
        "cot": ["khach_hang_id", "so_don"],
        "dap_an": """
SELECT khach_hang_id, COUNT(*) AS so_don
FROM don_hang
WHERE khach_hang_id IS NOT NULL
GROUP BY khach_hang_id
HAVING COUNT(*) >= 3;
""",
        "sai": [
            "SELECT khach_hang_id, COUNT(*) FROM don_hang GROUP BY khach_hang_id HAVING COUNT(*) >= 3",
            "SELECT khach_hang_id, COUNT(*) FROM don_hang WHERE khach_hang_id IS NOT NULL GROUP BY khach_hang_id"
            " HAVING COUNT(*) > 3",
        ],
    },
    {
        "id": "phong-luong-cao",
        "ten": "Phòng có lương trung bình trên 20 triệu",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mã phòng và lương trung bình** của các phòng có lương trung bình **trên 20 000 000**. Nhân viên chưa
xếp phòng (`phong_id` NULL) không thuộc phòng nào.

Điều kiện là trên **trung bình của nhóm**, không phải trên lương từng người.
""",
        "cot": ["phong_id", "luong_tb"],
        "dap_an": """
SELECT phong_id, AVG(luong) AS luong_tb
FROM nhan_vien
WHERE phong_id IS NOT NULL
GROUP BY phong_id
HAVING AVG(luong) > 20000000;
""",
        "sai": [
            "SELECT phong_id, AVG(luong) FROM nhan_vien WHERE luong > 20000000 AND phong_id IS NOT NULL"
            " GROUP BY phong_id",
            "SELECT phong_id, AVG(luong) FROM nhan_vien GROUP BY phong_id HAVING AVG(luong) > 20000000",
        ],
    },
    {
        "id": "doanh-thu-thang",
        "ten": "Doanh thu theo tháng năm 2024",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "thu_tu": True,
        "bang": ["don_hang", "chi_tiet_don"],
        "de": """
Tính **doanh thu từng tháng** của năm 2024, sắp theo tháng tăng dần. Doanh thu là tổng `so_luong * don_gia` của
các đơn **không bị huỷ** (`trang_thai` khác `huy`); tháng lấy theo `ngay_dat`. Chỉ liệt kê tháng có doanh thu.

Lấy tháng dạng `'08'` bằng hàm ngày của SQLite: `strftime('%m', ngay_dat)`; năm: `strftime('%Y', ngay_dat)`.
""",
        "cot": ["thang", "doanh_thu"],
        "dap_an": """
SELECT strftime('%m', d.ngay_dat) AS thang, SUM(c.so_luong * c.don_gia) AS doanh_thu
FROM don_hang d
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai <> 'huy' AND strftime('%Y', d.ngay_dat) = '2024'
GROUP BY strftime('%m', d.ngay_dat)
ORDER BY thang;
""",
        "sai": [
            "SELECT strftime('%m', d.ngay_dat), SUM(c.so_luong * c.don_gia) FROM don_hang d"
            " JOIN chi_tiet_don c ON c.don_hang_id = d.id GROUP BY strftime('%m', d.ngay_dat)"
            " ORDER BY strftime('%m', d.ngay_dat)",
            "SELECT strftime('%m', d.ngay_dat), SUM(c.don_gia) FROM don_hang d"
            " JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.trang_thai <> 'huy'"
            " GROUP BY strftime('%m', d.ngay_dat) ORDER BY strftime('%m', d.ngay_dat)",
        ],
    },
    {
        "id": "gio-du-an",
        "ten": "Tổng giờ mỗi dự án, từ 100 giờ",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["phan_cong"],
        "de": """
Tính **tổng số giờ** được phân công cho mỗi dự án, chỉ giữ dự án có tổng **từ 100 giờ trở lên**.
""",
        "cot": ["du_an_id", "tong_gio"],
        "dap_an": """
SELECT du_an_id, SUM(so_gio) AS tong_gio
FROM phan_cong
GROUP BY du_an_id
HAVING SUM(so_gio) >= 100;
""",
        "sai": [
            "SELECT du_an_id, SUM(so_gio) FROM phan_cong GROUP BY du_an_id HAVING SUM(so_gio) > 100",
            "SELECT du_an_id, SUM(so_gio) FROM phan_cong WHERE so_gio >= 100 GROUP BY du_an_id",
        ],
    },
    {
        "id": "mon-truot-nhieu",
        "ten": "Môn có nhiều lần thi dưới 4 điểm nhất",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua"],
        "them_du_lieu": KET_QUA_THEM,
        "de": """
Tìm môn có **nhiều lần thi dưới 4 điểm nhất** (đúng 4 điểm là đạt). Cho biết mã môn và số lần thi dưới 4. Nhiều
môn cùng nhiều nhất thì **in tất cả**.

`ORDER BY … LIMIT 1` chỉ giữ được một môn: dữ liệu mẫu có hai môn bằng nhau. So số lần của mỗi nhóm với số lớn
nhất, tính bằng một câu con.
""",
        "cot": ["mon_hoc_id", "so_lan"],
        "dap_an": """
SELECT mon_hoc_id, COUNT(*) AS so_lan
FROM ket_qua
WHERE diem < 4
GROUP BY mon_hoc_id
HAVING COUNT(*) = (
    SELECT COUNT(*) FROM ket_qua WHERE diem < 4
    GROUP BY mon_hoc_id ORDER BY COUNT(*) DESC LIMIT 1
);
""",
        "sai": [
            "SELECT mon_hoc_id, COUNT(*) FROM ket_qua WHERE diem < 4 GROUP BY mon_hoc_id ORDER BY COUNT(*) DESC LIMIT 1",
            "SELECT mon_hoc_id, COUNT(*) FROM ket_qua WHERE diem <= 4 GROUP BY mon_hoc_id HAVING COUNT(*) = ("
            "SELECT COUNT(*) FROM ket_qua WHERE diem <= 4 GROUP BY mon_hoc_id ORDER BY COUNT(*) DESC LIMIT 1)",
        ],
    },
    {
        "id": "ty-le-dat",
        "ten": "Tỉ lệ lần thi đạt của từng môn",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua"],
        "them_du_lieu": KET_QUA_THEM,
        "de": """
Với từng môn, tính **tỉ lệ phần trăm** lần thi **đạt** (điểm từ 4 trở lên) trên tổng số lần thi **có điểm**, làm
tròn 2 chữ số thập phân. Lần vắng thi (`diem` NULL) không tính vào cả tử lẫn mẫu. Chỉ liệt kê môn có ít nhất một
lần thi có điểm.

Chia hai số nguyên trong SQLite ra số nguyên (`1 / 3 = 0`): nhân với `100.0` trước khi chia. Làm tròn bằng
`ROUND(x, 2)`.
""",
        "cot": ["mon_hoc_id", "ty_le"],
        "dap_an": """
SELECT mon_hoc_id,
       ROUND(100.0 * SUM(CASE WHEN diem >= 4 THEN 1 ELSE 0 END) / COUNT(diem), 2) AS ty_le
FROM ket_qua
WHERE diem IS NOT NULL
GROUP BY mon_hoc_id;
""",
        "sai": [
            "SELECT mon_hoc_id, ROUND(100.0 * SUM(CASE WHEN diem >= 4 THEN 1 ELSE 0 END) / COUNT(*), 2)"
            " FROM ket_qua GROUP BY mon_hoc_id",
            "SELECT mon_hoc_id, ROUND(100 * SUM(CASE WHEN diem >= 4 THEN 1 ELSE 0 END) / COUNT(diem), 2)"
            " FROM ket_qua WHERE diem IS NOT NULL GROUP BY mon_hoc_id",
        ],
    },
]
