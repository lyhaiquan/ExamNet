"""sql.truy-van-con — truy vấn con một giá trị, IN, EXISTS, tương quan (docs/plans/danh-muc-bai.md)."""

BAI = [
    {
        "id": "sp-dat-nhat",
        "ten": "Sản phẩm có giá cao nhất",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "ban-hang",
        "bang": ["san_pham"],
        "de": """
Liệt kê **mã, tên, giá** của sản phẩm có **giá cao nhất**. Nếu nhiều sản phẩm cùng giá cao nhất thì **in tất cả**.

Câu con một giá trị `(SELECT MAX(gia) FROM san_pham)` tính giá cao nhất trước, câu ngoài lọc những sản phẩm có
đúng giá đó. `ORDER BY gia DESC LIMIT 1` chỉ ra **một** dòng nên sai khi có nhiều sản phẩm cùng giá cao nhất.
""",
        "them_du_lieu": """
INSERT INTO san_pham VALUES
    (11, 'MacBook Pro M3', 3, 28000000, 2);
""",
        "dap_an": """
SELECT id, ten, gia
FROM san_pham
WHERE gia = (SELECT MAX(gia) FROM san_pham);
""",
        "sai": [
            "SELECT id, ten, gia FROM san_pham ORDER BY gia DESC LIMIT 1",
            "SELECT id, ten, MAX(gia) FROM san_pham",
        ],
    },
    {
        "id": "sv-khoa-cntt",
        "ten": "Sinh viên thuộc các lớp khoa CNTT",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "lop"],
        "de": """
Liệt kê **mã và họ tên** các sinh viên thuộc **một lớp bất kỳ của khoa CNTT** (`lop.khoa = 'CNTT'`).

Khoa có nhiều lớp nên câu con `SELECT id FROM lop WHERE khoa = 'CNTT'` trả về **nhiều** giá trị: dùng `IN`, không
dùng `=`.
""",
        "dap_an": """
SELECT id, ho_ten
FROM sinh_vien
WHERE lop_id IN (SELECT id FROM lop WHERE khoa = 'CNTT');
""",
        "sai": [
            "SELECT id, ho_ten FROM sinh_vien WHERE lop_id = (SELECT id FROM lop WHERE khoa = 'CNTT')",
        ],
    },
    {
        "id": "khach-chua-dat",
        "ten": "Khách chưa đặt đơn nào",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang"],
        "de": """
Liệt kê **mã và họ tên** các khách hàng **chưa đặt đơn nào**.

Cẩn thận với `NOT IN`: bảng `don_hang` có đơn của khách vãng lai (`khach_hang_id` NULL). Khi danh sách trong
`NOT IN (...)` có NULL, phép so `x NOT IN (..., NULL)` không bao giờ ra *đúng*, nên câu ra **rỗng**. Dùng
`NOT EXISTS`, hoặc loại NULL khỏi câu con.
""",
        "dap_an": """
SELECT k.id, k.ho_ten
FROM khach_hang k
WHERE NOT EXISTS (SELECT 1 FROM don_hang d WHERE d.khach_hang_id = k.id);
""",
        "sai": [
            "SELECT id, ho_ten FROM khach_hang WHERE id NOT IN (SELECT khach_hang_id FROM don_hang)",
            "SELECT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id <> k.id",
        ],
    },
    {
        "id": "cao-nhat-phong",
        "ten": "Nhân viên lương cao nhất phòng mình",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mã, họ tên, mã phòng, lương** của các nhân viên có lương **cao nhất trong phòng của mình**. Cùng phòng có
nhiều người bằng lương cao nhất thì **in tất cả**. Nhân viên chưa xếp phòng không tính.

Đây là câu con **tương quan**: câu con nhắc tới dòng đang xét của câu ngoài
(`WHERE n2.phong_id = n.phong_id`), nên được tính lại cho từng nhân viên.
""",
        "dap_an": """
SELECT n.id, n.ho_ten, n.phong_id, n.luong
FROM nhan_vien n
WHERE n.luong = (SELECT MAX(n2.luong) FROM nhan_vien n2 WHERE n2.phong_id = n.phong_id);
""",
        "sai": [
            "SELECT id, ho_ten, phong_id, luong FROM nhan_vien WHERE luong = (SELECT MAX(luong) FROM nhan_vien)",
            "SELECT id, ho_ten, phong_id, luong FROM nhan_vien"
            " WHERE luong IN (SELECT MAX(luong) FROM nhan_vien GROUP BY phong_id)",
        ],
    },
    {
        "id": "tren-tb-mon",
        "ten": "Lần thi trên trung bình của môn",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua"],
        "de": """
Liệt kê các lần thi (**mã sinh viên, mã môn, lần thi, điểm**) có điểm **cao hơn điểm trung bình của chính môn đó**
(trung bình mọi lần thi của môn, `AVG` tự bỏ qua điểm NULL).

So với trung bình **cả bảng** là sai: mỗi môn có mức điểm riêng. Câu con phải tương quan theo `mon_hoc_id`.
""",
        "them_du_lieu": """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 5.0),
    (3, 2, 1, 6.5),
    (4, 2, 1, 4.0),
    (5, 2, 1, 7.0);
""",
        "dap_an": """
SELECT k.sinh_vien_id, k.mon_hoc_id, k.lan_thi, k.diem
FROM ket_qua k
WHERE k.diem > (SELECT AVG(k2.diem) FROM ket_qua k2 WHERE k2.mon_hoc_id = k.mon_hoc_id);
""",
        "sai": [
            "SELECT sinh_vien_id, mon_hoc_id, lan_thi, diem FROM ket_qua WHERE diem > (SELECT AVG(diem) FROM ket_qua)",
        ],
    },
    {
        "id": "lon-hon-moi-don",
        "ten": "Đơn lớn hơn mọi đơn của khách số 2",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang", "chi_tiet_don"],
        "de": """
Tổng tiền một đơn là tổng `so_luong * don_gia` các dòng chi tiết của đơn đó. Liệt kê **mã đơn và tổng tiền** của
các đơn có tổng tiền **lớn hơn mọi đơn** của khách hàng mã **2**. Nếu khách 2 chưa có đơn nào thì **mọi đơn** đều
thoả (lớn hơn mọi phần tử của tập rỗng).

SQLite không có `> ALL (...)`. Lớn hơn mọi đơn nghĩa là lớn hơn **đơn lớn nhất**: câu con một giá trị lấy tổng
tiền lớn nhất của khách 2 (`GROUP BY` đơn, `ORDER BY ... DESC LIMIT 1`); khách 2 không có đơn thì câu con ra NULL,
dùng `COALESCE(..., 0)`.
""",
        "cot": ["don_hang_id", "tong_tien"],
        "dap_an": """
SELECT c.don_hang_id, SUM(c.so_luong * c.don_gia) AS tong_tien
FROM chi_tiet_don c
GROUP BY c.don_hang_id
HAVING SUM(c.so_luong * c.don_gia) > COALESCE((
    SELECT SUM(c2.so_luong * c2.don_gia)
    FROM chi_tiet_don c2 JOIN don_hang d2 ON d2.id = c2.don_hang_id
    WHERE d2.khach_hang_id = 2
    GROUP BY c2.don_hang_id
    ORDER BY 1 DESC
    LIMIT 1), 0);
""",
        "sai": [
            "SELECT c.don_hang_id, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c GROUP BY c.don_hang_id"
            " HAVING SUM(c.so_luong * c.don_gia) > (SELECT SUM(c2.so_luong * c2.don_gia) FROM chi_tiet_don c2"
            " JOIN don_hang d2 ON d2.id = c2.don_hang_id WHERE d2.khach_hang_id = 2 GROUP BY c2.don_hang_id"
            " ORDER BY 1 LIMIT 1)",
            "SELECT c.don_hang_id, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c GROUP BY c.don_hang_id"
            " HAVING SUM(c.so_luong * c.don_gia) > (SELECT MAX(c2.so_luong * c2.don_gia) FROM chi_tiet_don c2"
            " JOIN don_hang d2 ON d2.id = c2.don_hang_id WHERE d2.khach_hang_id = 2)",
        ],
    },
    {
        "id": "ca-lop-deu-thi",
        "ten": "Môn mà mọi sinh viên lớp 1 đều đã thi",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["mon_hoc", "sinh_vien", "ket_qua"],
        "de": """
Liệt kê **mã và tên** các môn mà **mọi** sinh viên của lớp mã **1** đều đã thi (có ít nhất một dòng trong
`ket_qua` với môn đó, kể cả vắng thi).

"Mọi sinh viên đều đã thi" nghĩa là **không có** sinh viên nào của lớp 1 mà **không có** kết quả môn đó: hai
`NOT EXISTS` lồng nhau.
""",
        "them_du_lieu": """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 7.5),
    (3, 2, 1, 6.0),
    (4, 2, 1, 8.0);
""",
        "dap_an": """
SELECT m.id, m.ten_mon
FROM mon_hoc m
WHERE NOT EXISTS (
    SELECT 1 FROM sinh_vien s
    WHERE s.lop_id = 1
      AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id));
""",
        "sai": [
            "SELECT m.id, m.ten_mon FROM mon_hoc m WHERE EXISTS (SELECT 1 FROM ket_qua k"
            " JOIN sinh_vien s ON s.id = k.sinh_vien_id WHERE s.lop_id = 1 AND k.mon_hoc_id = m.id)",
            "SELECT m.id, m.ten_mon FROM mon_hoc m WHERE NOT EXISTS (SELECT 1 FROM sinh_vien s"
            " WHERE NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id))",
        ],
    },
    {
        "id": "phong-dong-nhat",
        "ten": "Phòng có nhiều nhân viên nhất",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["phong_ban", "nhan_vien"],
        "de": """
Liệt kê **mã phòng, tên phòng, số nhân viên** của phòng có **nhiều nhân viên nhất**. Nhiều phòng cùng số nhân viên
lớn nhất thì **in tất cả**. Nhân viên chưa xếp phòng không thuộc phòng nào.

Câu con trong `HAVING` tính số nhân viên lớn nhất của một phòng; câu ngoài giữ các phòng có đúng số đó.
""",
        "them_du_lieu": """
INSERT INTO nhan_vien VALUES
    (11, 'Lý Văn Vinh', 2, 5, 13000000, '2024-09-01');
""",
        "cot": ["id", "ten", "so_nhan_vien"],
        "dap_an": """
SELECT p.id, p.ten, COUNT(*) AS so_nhan_vien
FROM phong_ban p JOIN nhan_vien n ON n.phong_id = p.id
GROUP BY p.id, p.ten
HAVING COUNT(*) = (
    SELECT COUNT(*) FROM nhan_vien
    WHERE phong_id IS NOT NULL
    GROUP BY phong_id
    ORDER BY 1 DESC
    LIMIT 1);
""",
        "sai": [
            "SELECT p.id, p.ten, COUNT(*) FROM phong_ban p JOIN nhan_vien n ON n.phong_id = p.id"
            " GROUP BY p.id, p.ten ORDER BY COUNT(*) DESC LIMIT 1",
            "SELECT p.id, p.ten, COUNT(*) FROM phong_ban p JOIN nhan_vien n ON n.phong_id = p.id"
            " GROUP BY p.id, p.ten HAVING COUNT(*) = (SELECT COUNT(*) FROM nhan_vien GROUP BY phong_id"
            " ORDER BY 1 LIMIT 1)",
        ],
    },
    {
        "id": "ban-tren-tb",
        "ten": "Sản phẩm bán trên mức trung bình",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["chi_tiet_don"],
        "de": """
Tổng số lượng bán của một sản phẩm là tổng `so_luong` của nó trong `chi_tiet_don` (mọi đơn). Liệt kê **mã sản
phẩm và tổng số lượng bán** của các sản phẩm có tổng số lượng bán **lớn hơn trung bình** tổng số lượng bán của
**các sản phẩm đã bán**.

Trung bình đó là tổng số lượng chia cho **số sản phẩm khác nhau** đã bán, không phải `AVG(so_luong)` (trung bình
trên từng **dòng** chi tiết).
""",
        "them_du_lieu": """
INSERT INTO chi_tiet_don VALUES
    (7, 1, 1, 22000000),
    (9, 1, 1, 22000000),
    (10, 1, 1, 22000000);
""",
        "cot": ["san_pham_id", "tong_so_luong"],
        "dap_an": """
SELECT san_pham_id, SUM(so_luong) AS tong_so_luong
FROM chi_tiet_don
GROUP BY san_pham_id
HAVING SUM(so_luong) > (SELECT SUM(so_luong) * 1.0 / COUNT(DISTINCT san_pham_id) FROM chi_tiet_don);
""",
        "sai": [
            "SELECT san_pham_id, SUM(so_luong) FROM chi_tiet_don GROUP BY san_pham_id"
            " HAVING SUM(so_luong) > (SELECT AVG(so_luong) FROM chi_tiet_don)",
        ],
    },
    {
        "id": "quan-ly-nhieu",
        "ten": "Người quản lý từ 3 nhân viên trở lên",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mã và họ tên** những người **quản lý trực tiếp từ 3 nhân viên trở lên** (có ít nhất 3 nhân viên có
`quan_ly_id` là mã của người đó).
""",
        "them_du_lieu": """
INSERT INTO nhan_vien VALUES
    (11, 'Lý Văn Vinh', 2, 5, 13000000, '2024-09-01');
""",
        "dap_an": """
SELECT id, ho_ten
FROM nhan_vien
WHERE id IN (SELECT quan_ly_id FROM nhan_vien GROUP BY quan_ly_id HAVING COUNT(*) >= 3);
""",
        "sai": [
            "SELECT id, ho_ten FROM nhan_vien"
            " WHERE id IN (SELECT quan_ly_id FROM nhan_vien GROUP BY quan_ly_id HAVING COUNT(*) > 3)",
            "SELECT id, ho_ten FROM nhan_vien"
            " WHERE quan_ly_id IN (SELECT quan_ly_id FROM nhan_vien GROUP BY quan_ly_id HAVING COUNT(*) >= 3)",
        ],
    },
    {
        "id": "luong-thu-hai",
        "ten": "Mức lương cao thứ hai",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Cho biết **mức lương cao thứ hai** trong công ty: mức lương lớn nhất trong các mức **nhỏ hơn** lương cao nhất.
Nhiều người cùng lương cao nhất thì vẫn tính là một mức. Không có mức thứ hai thì in NULL. **Không dùng
`LIMIT`/`OFFSET`.**

Kết quả là một dòng, một cột.
""",
        "cot": ["luong_thu_hai"],
        "dap_an": """
SELECT MAX(luong) AS luong_thu_hai
FROM nhan_vien
WHERE luong < (SELECT MAX(luong) FROM nhan_vien);
""",
        "sai": [
            "SELECT luong FROM nhan_vien ORDER BY luong DESC LIMIT 1 OFFSET 1",
            "SELECT MAX(luong) FROM nhan_vien WHERE luong <= (SELECT MAX(luong) FROM nhan_vien)",
        ],
    },
    {
        "id": "mua-moi-danh-muc",
        "ten": "Khách đã mua ở mọi danh mục",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "danh_muc", "san_pham", "don_hang", "chi_tiet_don"],
        "de": """
Chỉ xét các danh mục **có sản phẩm** (có ít nhất một dòng `san_pham` với `danh_muc_id` là danh mục đó). Liệt kê
**mã và họ tên** các khách đã mua (trong đơn bất kỳ, kể cả đơn huỷ) **ít nhất một sản phẩm của mỗi danh mục đó**.

Đây là phép **chia**: không có danh mục (có sản phẩm) nào mà khách **chưa** mua — hai `NOT EXISTS` lồng nhau.
""",
        "dap_an": """
SELECT k.id, k.ho_ten
FROM khach_hang k
WHERE NOT EXISTS (
    SELECT 1 FROM danh_muc dm
    WHERE EXISTS (SELECT 1 FROM san_pham sp WHERE sp.danh_muc_id = dm.id)
      AND NOT EXISTS (
          SELECT 1
          FROM don_hang d
          JOIN chi_tiet_don c ON c.don_hang_id = d.id
          JOIN san_pham s ON s.id = c.san_pham_id
          WHERE d.khach_hang_id = k.id AND s.danh_muc_id = dm.id));
""",
        "sai": [
            "SELECT k.id, k.ho_ten FROM khach_hang k WHERE NOT EXISTS (SELECT 1 FROM danh_muc dm WHERE NOT EXISTS ("
            "SELECT 1 FROM don_hang d JOIN chi_tiet_don c ON c.don_hang_id = d.id JOIN san_pham s"
            " ON s.id = c.san_pham_id WHERE d.khach_hang_id = k.id AND s.danh_muc_id = dm.id))",
            "SELECT DISTINCT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id",
        ],
    },
    {
        "id": "du-an-vuot-ngan-sach",
        "ten": "Phòng có dự án vượt ngân sách phòng",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["phong_ban", "du_an"],
        "de": """
Liệt kê **mã, tên, ngân sách** của các phòng mà **tổng ngân sách các dự án của phòng** lớn hơn **ngân sách của
phòng**. Phòng không có dự án nào thì không tính.
""",
        "dap_an": """
SELECT p.id, p.ten, p.ngan_sach
FROM phong_ban p
WHERE p.ngan_sach < (SELECT SUM(d.ngan_sach) FROM du_an d WHERE d.phong_id = p.id);
""",
        "sai": [
            "SELECT p.id, p.ten, p.ngan_sach FROM phong_ban p WHERE p.ngan_sach < (SELECT SUM(ngan_sach) FROM du_an)",
        ],
    },
]
