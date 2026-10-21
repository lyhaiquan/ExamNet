"""sql.dml — INSERT, UPDATE, DELETE (docs/plans/danh-muc-bai.md)."""

BAI = [
    {
        "id": "them-sv",
        "ten": "Thêm một sinh viên mới",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "loai_cau": "dml",
        "bang": ["sinh_vien"],
        "de": """
Thêm vào bảng `sinh_vien` sinh viên mới: mã **100**, họ tên **Lý Thị Hoa**, giới tính **Nữ**, ngày sinh
**2005-06-01**, quê **Bắc Ninh**, thuộc lớp có mã **1**.

Nên ghi rõ danh sách cột: `INSERT INTO sinh_vien (id, ho_ten, ...) VALUES (...)`. Giá trị phải đúng thứ tự các
cột đã ghi; chuỗi và ngày đặt trong nháy đơn.
""",
        "dap_an": """
INSERT INTO sinh_vien (id, ho_ten, gioi_tinh, ngay_sinh, que_quan, lop_id)
VALUES (100, 'Lý Thị Hoa', 'Nữ', '2005-06-01', 'Bắc Ninh', 1);
""",
        "sai": [
            "INSERT INTO sinh_vien (id, ho_ten, gioi_tinh, ngay_sinh, lop_id)"
            " VALUES (100, 'Lý Thị Hoa', 'Nữ', '2005-06-01', 1)",
            "INSERT INTO sinh_vien (id, ho_ten, gioi_tinh, ngay_sinh, que_quan, lop_id)"
            " VALUES (100, 'Lý Thị Hoa', 'Nữ', '2005-01-06', 'Bắc Ninh', 1)",
        ],
    },
    {
        "id": "xoa-don-huy",
        "ten": "Xoá chi tiết của các đơn đã huỷ",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "loai_cau": "dml",
        "bang": ["don_hang", "chi_tiet_don"],
        "de": """
Xoá khỏi bảng `chi_tiet_don` mọi dòng thuộc các đơn hàng có trạng thái **`huy`**. Bảng `don_hang` giữ nguyên.

Mã các đơn đã huỷ khác nhau giữa các bộ dữ liệu ẩn: đừng ghi cứng `IN (3, 8)`, hãy lấy chúng bằng một câu con
`(SELECT id FROM don_hang WHERE trang_thai = 'huy')`.
""",
        "dap_an": """
DELETE FROM chi_tiet_don
WHERE don_hang_id IN (SELECT id FROM don_hang WHERE trang_thai = 'huy');
""",
        "sai": [
            "DELETE FROM chi_tiet_don WHERE don_hang_id IN (3, 8)",
            "DELETE FROM don_hang WHERE trang_thai = 'huy'",
        ],
    },
    {
        "id": "tru-ton-kho",
        "ten": "Trừ tồn kho theo chi tiết đơn số 4",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "loai_cau": "dml",
        "bang": ["san_pham", "chi_tiet_don"],
        "de": """
Đơn hàng mã **4** vừa xuất kho. Với mỗi sản phẩm có trong đơn 4, trừ `ton_kho` đi **tổng số lượng** sản phẩm đó
trong đơn. Sản phẩm không có trong đơn 4 giữ nguyên.

Số cần trừ của từng sản phẩm lấy bằng câu con **tương quan** trong `SET`:
`(SELECT SUM(so_luong) FROM chi_tiet_don c WHERE c.don_hang_id = 4 AND c.san_pham_id = san_pham.id)`.
Nhớ thêm `WHERE` để chỉ đổi sản phẩm có trong đơn: với sản phẩm khác, câu con ra NULL.
""",
        "dap_an": """
UPDATE san_pham
SET ton_kho = ton_kho - (SELECT SUM(so_luong) FROM chi_tiet_don c
                         WHERE c.don_hang_id = 4 AND c.san_pham_id = san_pham.id)
WHERE id IN (SELECT san_pham_id FROM chi_tiet_don WHERE don_hang_id = 4);
""",
        "sai": [
            "UPDATE san_pham SET ton_kho = ton_kho - 1"
            " WHERE id IN (SELECT san_pham_id FROM chi_tiet_don WHERE don_hang_id = 4)",
            "UPDATE san_pham SET ton_kho = ton_kho - 2 WHERE id = 3",
        ],
    },
    {
        "id": "giam-gia",
        "ten": "Giảm 5% giá sản phẩm danh mục Điện thoại",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "loai_cau": "dml",
        "bang": ["danh_muc", "san_pham"],
        "de": """
Giảm **5%** giá mọi sản phẩm thuộc danh mục có tên **Điện thoại** (chỉ danh mục đó, không tính danh mục cha
"Điện tử"). Tìm mã danh mục theo tên bằng câu con.

Giảm 5% nghĩa là giá mới bằng `gia * 0.95`.
""",
        "dap_an": """
UPDATE san_pham
SET gia = gia * 0.95
WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện thoại');
""",
        "sai": [
            "UPDATE san_pham SET gia = gia * 0.05"
            " WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện thoại')",
            "UPDATE san_pham SET gia = gia * 0.95"
            " WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện tử')",
            "UPDATE san_pham SET gia = gia - 5"
            " WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện thoại')",
        ],
    },
    {
        "id": "chuyen-phong",
        "ten": "Chuyển nhân viên phòng Nhân sự sang Pháp chế",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "loai_cau": "dml",
        "bang": ["phong_ban", "nhan_vien"],
        "de": """
Phòng **Nhân sự** sáp nhập vào phòng **Pháp chế**: chuyển mọi nhân viên của phòng Nhân sự sang phòng Pháp chế.

Số hiệu các phòng thay đổi ở các bộ dữ liệu ẩn: tìm cả hai phòng theo **tên** bằng câu con, một câu trong `SET`,
một câu trong `WHERE`.
""",
        "dap_an": """
UPDATE nhan_vien
SET phong_id = (SELECT id FROM phong_ban WHERE ten = 'Pháp chế')
WHERE phong_id = (SELECT id FROM phong_ban WHERE ten = 'Nhân sự');
""",
        "sai": [
            "UPDATE nhan_vien SET phong_id = 4 WHERE phong_id = 3",
            "UPDATE nhan_vien SET phong_id = (SELECT id FROM phong_ban WHERE ten = 'Pháp chế')",
        ],
    },
    {
        "id": "them-tu-truy-van",
        "ten": "Chép sinh viên đạt học bổng sang bảng hoc_bong",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "loai_cau": "dml",
        "bang": ["sinh_vien", "ket_qua", "hoc_bong"],
        "ghi_chu_bang": {"hoc_bong": "bảng riêng của bài, đang rỗng"},
        "them_schema": """
CREATE TABLE hoc_bong (
    sinh_vien_id  INTEGER NOT NULL REFERENCES sinh_vien (id),
    ho_ten        TEXT NOT NULL,
    diem          REAL NOT NULL
);
""",
        "de": """
Sinh viên đạt học bổng là người có điểm **lần thi 1** môn **Lập trình mạng** (`mon_hoc_id = 1`) **từ 8 trở
lên**. Chép mỗi lần thi như vậy thành một dòng của bảng `hoc_bong`: mã sinh viên, họ tên, điểm.

Dùng một câu `INSERT INTO hoc_bong (...) SELECT ...`: phần `SELECT` (có `JOIN` để lấy họ tên) cho ra đúng các
dòng cần thêm.
""",
        "dap_an": """
INSERT INTO hoc_bong (sinh_vien_id, ho_ten, diem)
SELECT s.id, s.ho_ten, k.diem
FROM sinh_vien s
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.mon_hoc_id = 1 AND k.lan_thi = 1 AND k.diem >= 8;
""",
        "sai": [
            "INSERT INTO hoc_bong (sinh_vien_id, ho_ten, diem) SELECT s.id, s.ho_ten, k.diem FROM sinh_vien s"
            " JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.mon_hoc_id = 1 AND k.lan_thi = 1 AND k.diem > 8",
            "INSERT INTO hoc_bong (sinh_vien_id, ho_ten, diem) SELECT s.id, s.ho_ten, k.diem FROM sinh_vien s"
            " JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem >= 8",
        ],
    },
]
