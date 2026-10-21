"""sql.ket-noi — JOIN: nối bảng, LEFT JOIN, tự nối, FULL JOIN, CROSS JOIN (docs/plans/danh-muc-bai.md)."""

BAI = [
    {
        "id": "bang-diem",
        "ten": "Bảng điểm mọi lần thi",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "truong-hoc",
        "bang": ["ket_qua", "sinh_vien", "mon_hoc"],
        "de": """
Liệt kê **họ tên sinh viên, tên môn, điểm** của **mọi lần thi** trong `ket_qua` (thi lại cũng là một dòng riêng).

Cần nối **3 bảng**: mỗi dòng `ket_qua` ghép với sinh viên có `id = sinh_vien_id` và môn có `id = mon_hoc_id`.
Mỗi `JOIN` có điều kiện `ON` riêng của nó.
""",
        "them_du_lieu": """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 7.5),
    (4, 3, 1, 3.5);
""",
        "dap_an": """
SELECT s.ho_ten, m.ten_mon, k.diem
FROM ket_qua k
JOIN sinh_vien s ON s.id = k.sinh_vien_id
JOIN mon_hoc m ON m.id = k.mon_hoc_id;
""",
        "sai": [
            "SELECT s.ho_ten, m.ten_mon, k.diem FROM ket_qua k JOIN sinh_vien s ON s.id = k.sinh_vien_id"
            " JOIN mon_hoc m ON m.id = k.sinh_vien_id",
            "SELECT s.ho_ten, m.ten_mon, k.diem FROM ket_qua k JOIN sinh_vien s ON s.id = k.sinh_vien_id"
            " JOIN mon_hoc m ON m.id = k.mon_hoc_id WHERE k.lan_thi = 1",
        ],
    },
    {
        "id": "sp-danh-muc",
        "ten": "Sản phẩm kèm tên danh mục",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham", "danh_muc"],
        "de": """
Liệt kê **tên sản phẩm** kèm **tên danh mục** của sản phẩm đó.

Hai bảng đều có cột `ten`: viết rõ `s.ten`, `d.ten` (đặt bí danh cho bảng) để không nhầm.
""",
        "dap_an": """
SELECT s.ten, d.ten
FROM san_pham s
JOIN danh_muc d ON d.id = s.danh_muc_id;
""",
        "sai": [
            "SELECT s.ten, d.ten FROM san_pham s JOIN danh_muc d ON d.id = s.id",
            "SELECT s.ten, d.ten FROM san_pham s JOIN danh_muc d ON d.cha_id = s.danh_muc_id",
        ],
    },
    {
        "id": "don-ha-noi",
        "ten": "Đơn hàng của khách ở Hà Nội",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang", "khach_hang"],
        "de": """
Liệt kê **mã đơn, ngày đặt, họ tên khách** của các đơn hàng do khách ở **Hà Nội** đặt.

Nối `don_hang` với `khach_hang` rồi lọc bằng `WHERE` trên cột của bảng khách.
""",
        "dap_an": """
SELECT d.id, d.ngay_dat, k.ho_ten
FROM don_hang d
JOIN khach_hang k ON k.id = d.khach_hang_id
WHERE k.thanh_pho = 'Hà Nội';
""",
        "sai": [
            "SELECT d.id, d.ngay_dat, k.ho_ten FROM don_hang d JOIN khach_hang k ON k.id = d.khach_hang_id",
            "SELECT d.id, d.ngay_dat, k.ho_ten FROM don_hang d JOIN khach_hang k ON k.id = d.id"
            " WHERE k.thanh_pho = 'Hà Nội'",
        ],
    },
    {
        "id": "mon-chua-thi",
        "ten": "Môn chưa có ai thi",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "truong-hoc",
        "bang": ["mon_hoc", "ket_qua"],
        "de": """
Liệt kê **mã môn và tên môn** của các môn **chưa có lần thi nào** trong `ket_qua`.

`mon_hoc LEFT JOIN ket_qua` giữ **mọi** môn; môn không ghép được lần thi nào có các cột của `ket_qua` là NULL.
Lọc những dòng đó bằng một cột **không bao giờ NULL khi ghép được**, như `k.mon_hoc_id`. Cẩn thận: `k.diem` NULL
còn có thể là lần thi **vắng mặt**, môn đó vẫn đã có người thi.
""",
        "dap_an": """
SELECT m.ma_mon, m.ten_mon
FROM mon_hoc m
LEFT JOIN ket_qua k ON k.mon_hoc_id = m.id
WHERE k.mon_hoc_id IS NULL;
""",
        "sai": [
            "SELECT m.ma_mon, m.ten_mon FROM mon_hoc m LEFT JOIN ket_qua k ON k.mon_hoc_id = m.id"
            " WHERE k.diem IS NULL",
            "SELECT m.ma_mon, m.ten_mon FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id"
            " WHERE k.diem IS NULL",
        ],
    },
    {
        "id": "lop-gvcn",
        "ten": "Lớp kèm giáo viên chủ nhiệm",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["lop", "giang_vien"],
        "de": """
Liệt kê **tên lớp** kèm **họ tên giáo viên chủ nhiệm**. Lớp chưa có chủ nhiệm **vẫn phải có** trong kết quả, cột họ
tên để NULL.

`JOIN` thường bỏ mất lớp có `gvcn_id` NULL; `LEFT JOIN` giữ mọi dòng của bảng bên trái.
""",
        "dap_an": """
SELECT l.ten_lop, g.ho_ten
FROM lop l
LEFT JOIN giang_vien g ON g.id = l.gvcn_id;
""",
        "sai": [
            "SELECT l.ten_lop, g.ho_ten FROM lop l JOIN giang_vien g ON g.id = l.gvcn_id",
            "SELECT l.ten_lop, g.ho_ten FROM giang_vien g LEFT JOIN lop l ON g.id = l.gvcn_id",
        ],
    },
    {
        "id": "cung-phong",
        "ten": "Các cặp nhân viên cùng phòng",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mọi cặp nhân viên cùng phòng**: mã người thứ nhất, mã người thứ hai, mã phòng. **Mỗi cặp chỉ một lần**,
người có mã nhỏ hơn đứng trước. Nhân viên chưa xếp phòng không cùng phòng với ai.

Đây là **tự nối**: nối `nhan_vien` với chính nó qua hai bí danh `a`, `b`. Điều kiện `a.id < b.id` vừa bỏ cặp một
người với chính mình, vừa bỏ cặp lặp ngược (B, A).
""",
        "dap_an": """
SELECT a.id, b.id, a.phong_id
FROM nhan_vien a
JOIN nhan_vien b ON b.phong_id = a.phong_id AND a.id < b.id;
""",
        "sai": [
            "SELECT a.id, b.id, a.phong_id FROM nhan_vien a JOIN nhan_vien b"
            " ON b.phong_id = a.phong_id AND a.id <> b.id",
            "SELECT a.id, b.id, a.phong_id FROM nhan_vien a JOIN nhan_vien b ON b.phong_id = a.phong_id",
        ],
    },
    {
        "id": "hon-quan-ly",
        "ten": "Nhân viên lương cao hơn quản lý",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mã, họ tên, lương** của nhân viên kèm **lương người quản lý trực tiếp**, chỉ những người có lương **cao
hơn hẳn** lương quản lý của mình (bằng thì không tính).

Tự nối: `nv` là nhân viên, `ql` là quản lý, ghép theo `ql.id = nv.quan_ly_id`.
""",
        "them_du_lieu": """
INSERT INTO nhan_vien VALUES
    (11, 'Lý Thị Vân', 2, 6, 17000000, '2023-01-05'),
    (12, 'Mai Văn Xuân', 3, 8, 16000000, '2024-09-02');
""",
        "dap_an": """
SELECT nv.id, nv.ho_ten, nv.luong, ql.luong
FROM nhan_vien nv
JOIN nhan_vien ql ON ql.id = nv.quan_ly_id
WHERE nv.luong > ql.luong;
""",
        "sai": [
            "SELECT nv.id, nv.ho_ten, nv.luong, ql.luong FROM nhan_vien nv JOIN nhan_vien ql"
            " ON ql.id = nv.quan_ly_id WHERE nv.luong >= ql.luong",
            "SELECT nv.id, nv.ho_ten, nv.luong, ql.luong FROM nhan_vien nv JOIN nhan_vien ql"
            " ON nv.id = ql.quan_ly_id WHERE nv.luong > ql.luong",
        ],
    },
    {
        "id": "danh-muc-cha",
        "ten": "Danh mục kèm danh mục cha",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["danh_muc"],
        "de": """
Liệt kê **tên danh mục** kèm **tên danh mục cha**. Danh mục gốc (không có cha) vẫn có trong kết quả, tên cha để
NULL.

Tự nối `danh_muc` với chính nó: `con.cha_id = cha.id`, và dùng `LEFT JOIN` để giữ danh mục gốc.
""",
        "dap_an": """
SELECT con.ten, cha.ten
FROM danh_muc con
LEFT JOIN danh_muc cha ON cha.id = con.cha_id;
""",
        "sai": [
            "SELECT con.ten, cha.ten FROM danh_muc con JOIN danh_muc cha ON cha.id = con.cha_id",
            "SELECT con.ten, cha.ten FROM danh_muc con LEFT JOIN danh_muc cha ON con.id = cha.cha_id",
        ],
    },
    {
        "id": "nv-du-an",
        "ten": "Nhân viên kèm dự án và số giờ",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["phan_cong", "nhan_vien", "du_an"],
        "de": """
Liệt kê **họ tên nhân viên, tên dự án, số giờ** của mọi dòng phân công.

`phan_cong` là bảng nối nhiều–nhiều: mỗi dòng ghép một nhân viên với một dự án.
""",
        "dap_an": """
SELECT n.ho_ten, d.ten, p.so_gio
FROM phan_cong p
JOIN nhan_vien n ON n.id = p.nhan_vien_id
JOIN du_an d ON d.id = p.du_an_id;
""",
        "sai": [
            "SELECT n.ho_ten, d.ten, p.so_gio FROM phan_cong p JOIN nhan_vien n ON n.id = p.nhan_vien_id"
            " JOIN du_an d ON d.phong_id = n.phong_id",
            "SELECT n.ho_ten, d.ten, p.so_gio FROM phan_cong p JOIN nhan_vien n ON n.id = p.du_an_id"
            " JOIN du_an d ON d.id = p.nhan_vien_id",
        ],
    },
    {
        "id": "du-an-trong",
        "ten": "Dự án chưa có ai phân công",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["du_an", "phan_cong"],
        "de": """
Liệt kê **mã và tên** các dự án **chưa có nhân viên nào** được phân công.

`du_an LEFT JOIN phan_cong` rồi giữ những dự án không ghép được dòng phân công nào.
""",
        "them_du_lieu": """
INSERT INTO du_an VALUES
    (5, 'Kho dữ liệu', 1, 300000000);
""",
        "dap_an": """
SELECT d.id, d.ten
FROM du_an d
LEFT JOIN phan_cong p ON p.du_an_id = d.id
WHERE p.du_an_id IS NULL;
""",
        "sai": [
            "SELECT d.id, d.ten FROM du_an d JOIN phan_cong p ON p.du_an_id = d.id WHERE p.nhan_vien_id IS NULL",
            "SELECT d.id, d.ten FROM du_an d LEFT JOIN phan_cong p ON p.du_an_id = d.id WHERE d.phong_id IS NULL",
        ],
    },
    {
        "id": "phong-va-du-an",
        "ten": "Ghép phòng và dự án, giữ cả hai phía",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["phong_ban", "du_an"],
        "de": """
Ghép **tên phòng** với **tên dự án** của phòng đó. Giữ **cả hai phía**: phòng chưa có dự án nào vẫn hiện (tên dự án
NULL), dự án chưa giao phòng nào vẫn hiện (tên phòng NULL).

`FULL OUTER JOIN` = `LEFT JOIN` cộng thêm các dòng bên phải không ghép được. SQLite từ bản 3.39 có `FULL JOIN`;
cách khác là `LEFT JOIN … UNION ALL` phần dự án không có phòng.
""",
        "dap_an": """
SELECT p.ten, d.ten
FROM phong_ban p
FULL OUTER JOIN du_an d ON d.phong_id = p.id;
""",
        "sai": [
            "SELECT p.ten, d.ten FROM phong_ban p LEFT JOIN du_an d ON d.phong_id = p.id",
            "SELECT p.ten, d.ten FROM phong_ban p JOIN du_an d ON d.phong_id = p.id",
        ],
    },
    {
        "id": "lich-thi",
        "ten": "Mọi cặp lớp và môn để lập lịch thi",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["lop", "mon_hoc"],
        "de": """
Để lập lịch thi, liệt kê **mọi cặp (tên lớp, tên môn)**: lớp nào cũng thi mọi môn.

`CROSS JOIN` ghép **mỗi** dòng bảng này với **mỗi** dòng bảng kia, không cần điều kiện: 4 lớp × 4 môn = 16 dòng.
""",
        "dap_an": """
SELECT l.ten_lop, m.ten_mon
FROM lop l
CROSS JOIN mon_hoc m;
""",
        "sai": [
            "SELECT l.ten_lop, m.ten_mon FROM lop l JOIN mon_hoc m ON m.id = l.id",
        ],
    },
    {
        "id": "sp-chua-ban",
        "ten": "Sản phẩm chưa từng được bán",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham", "chi_tiet_don"],
        "de": """
Liệt kê **mã và tên** các sản phẩm **chưa từng xuất hiện** trong chi tiết đơn nào (đơn huỷ cũng tính là đã xuất
hiện).
""",
        "them_du_lieu": """
INSERT INTO san_pham VALUES
    (11, 'Tai nghe Bluetooth', 1, 900000, 15),
    (12, 'Sách SQL nâng cao', 5, 210000, 0);
""",
        "dap_an": """
SELECT s.id, s.ten
FROM san_pham s
LEFT JOIN chi_tiet_don c ON c.san_pham_id = s.id
WHERE c.san_pham_id IS NULL;
""",
        "sai": [
            "SELECT s.id, s.ten FROM san_pham s JOIN chi_tiet_don c ON c.san_pham_id = s.id"
            " WHERE c.so_luong IS NULL",
            "SELECT s.id, s.ten FROM san_pham s WHERE s.ton_kho = 0",
        ],
    },
    {
        "id": "tong-chi",
        "ten": "Tổng tiền mỗi khách đã chi",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang", "chi_tiet_don"],
        "de": """
Với **mọi khách hàng**, cho biết **mã, họ tên, tổng tiền đã chi** (tổng `so_luong * don_gia` của các đơn **không bị
huỷ**). Khách chưa mua gì, hoặc chỉ có đơn bị huỷ, hiện tổng **0**.

Hai bẫy: điều kiện `trang_thai <> 'huy'` đặt ở `WHERE` sẽ xoá luôn khách không có đơn hợp lệ — hãy đặt nó trong
`ON` của `LEFT JOIN`; và `SUM` của toàn NULL là NULL, cần `COALESCE(…, 0)`.
""",
        "dap_an": """
SELECT k.id, k.ho_ten, COALESCE(SUM(c.so_luong * c.don_gia), 0) AS tong_chi
FROM khach_hang k
LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy'
LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id
GROUP BY k.id, k.ho_ten;
""",
        "sai": [
            "SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) FROM khach_hang k"
            " LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy'"
            " LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id GROUP BY k.id, k.ho_ten",
            "SELECT k.id, k.ho_ten, COALESCE(SUM(c.so_luong * c.don_gia), 0) FROM khach_hang k"
            " LEFT JOIN don_hang d ON d.khach_hang_id = k.id LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten",
            "SELECT k.id, k.ho_ten, COALESCE(SUM(c.so_luong * c.don_gia), 0) FROM khach_hang k"
            " LEFT JOIN don_hang d ON d.khach_hang_id = k.id LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " GROUP BY k.id, k.ho_ten",
        ],
    },
    {
        "id": "don-nhieu-sp",
        "ten": "Đơn có từ 3 sản phẩm khác nhau",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang", "chi_tiet_don", "khach_hang"],
        "de": """
Liệt kê **mã đơn, họ tên khách, số sản phẩm khác nhau** của các đơn có **từ 3 sản phẩm khác nhau** trở lên. Một
sản phẩm ghi hai dòng trong cùng đơn chỉ tính một. Đơn của khách vãng lai vẫn tính, họ tên để NULL.
""",
        "them_du_lieu": """
INSERT INTO chi_tiet_don VALUES
    (9, 1, 1, 22000000),
    (9, 3, 1, 5000000),
    (4, 3, 1, 5000000),
    (4, 10, 1, 600000);
""",
        "dap_an": """
SELECT d.id, k.ho_ten, COUNT(DISTINCT c.san_pham_id) AS so_san_pham
FROM don_hang d
JOIN chi_tiet_don c ON c.don_hang_id = d.id
LEFT JOIN khach_hang k ON k.id = d.khach_hang_id
GROUP BY d.id, k.ho_ten
HAVING COUNT(DISTINCT c.san_pham_id) >= 3;
""",
        "sai": [
            "SELECT d.id, k.ho_ten, COUNT(*) FROM don_hang d JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " LEFT JOIN khach_hang k ON k.id = d.khach_hang_id GROUP BY d.id, k.ho_ten HAVING COUNT(*) >= 3",
        ],
    },
    {
        "id": "cung-que",
        "ten": "Cặp sinh viên cùng quê khác lớp",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien"],
        "de": """
Liệt kê **mọi cặp sinh viên cùng quê nhưng khác lớp**: mã người thứ nhất, mã người thứ hai, quê quán. Mỗi cặp một
lần, mã nhỏ hơn đứng trước. Chỉ xét sinh viên **đã xếp lớp** và **có ghi quê**.
""",
        "dap_an": """
SELECT a.id, b.id, a.que_quan
FROM sinh_vien a
JOIN sinh_vien b ON b.que_quan = a.que_quan AND a.id < b.id
WHERE a.lop_id <> b.lop_id;
""",
        "sai": [
            "SELECT a.id, b.id, a.que_quan FROM sinh_vien a JOIN sinh_vien b"
            " ON b.que_quan = a.que_quan AND a.id <> b.id WHERE a.lop_id <> b.lop_id",
            "SELECT a.id, b.id, a.que_quan FROM sinh_vien a JOIN sinh_vien b"
            " ON b.que_quan = a.que_quan AND a.id < b.id WHERE a.lop_id <> b.lop_id OR a.lop_id IS NULL",
        ],
    },
    {
        "id": "doanh-thu-danh-muc-cha",
        "ten": "Doanh thu theo danh mục cha",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["danh_muc", "san_pham", "don_hang", "chi_tiet_don"],
        "de": """
Tính **doanh thu** (tổng `so_luong * don_gia`) của các đơn **đã giao** (`da-giao`) theo **danh mục cha**: mỗi sản
phẩm tính vào cha của danh mục chứa nó; nếu danh mục của sản phẩm là danh mục gốc thì tính vào chính danh mục đó.
Kết quả: tên danh mục cha, doanh thu. Danh mục không có doanh thu không cần hiện.

Gợi ý: nối `danh_muc` hai lần, lần hai theo `COALESCE(m.cha_id, m.id)`.
""",
        "dap_an": """
SELECT g.ten, SUM(c.so_luong * c.don_gia) AS doanh_thu
FROM chi_tiet_don c
JOIN don_hang d ON d.id = c.don_hang_id
JOIN san_pham s ON s.id = c.san_pham_id
JOIN danh_muc m ON m.id = s.danh_muc_id
JOIN danh_muc g ON g.id = COALESCE(m.cha_id, m.id)
WHERE d.trang_thai = 'da-giao'
GROUP BY g.id, g.ten;
""",
        "sai": [
            "SELECT g.ten, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " JOIN san_pham s ON s.id = c.san_pham_id JOIN danh_muc m ON m.id = s.danh_muc_id"
            " JOIN danh_muc g ON g.id = m.cha_id WHERE d.trang_thai = 'da-giao' GROUP BY g.id, g.ten",
            "SELECT g.ten, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " JOIN san_pham s ON s.id = c.san_pham_id JOIN danh_muc m ON m.id = s.danh_muc_id"
            " JOIN danh_muc g ON g.id = COALESCE(m.cha_id, m.id) GROUP BY g.id, g.ten",
        ],
    },
    {
        "id": "mua-ca-hai",
        "ten": "Khách đã mua cả sản phẩm 1 và 2",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang", "chi_tiet_don"],
        "de": """
Liệt kê **mã và họ tên** các khách đã mua **cả** sản phẩm mã **1** **lẫn** sản phẩm mã **2** (có thể ở hai đơn khác
nhau; tính mọi đơn, kể cả đơn huỷ).

Một dòng chi tiết chỉ có một sản phẩm, nên `WHERE san_pham_id = 1 AND san_pham_id = 2` không bao giờ đúng. Lọc
`IN (1, 2)` rồi gộp theo khách và đếm số sản phẩm **khác nhau**.
""",
        "them_du_lieu": """
INSERT INTO chi_tiet_don VALUES
    (9, 2, 1, 20000000),
    (7, 1, 1, 22000000),
    (2, 1, 1, 22000000);
""",
        "dap_an": """
SELECT k.id, k.ho_ten
FROM khach_hang k
JOIN don_hang d ON d.khach_hang_id = k.id
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE c.san_pham_id IN (1, 2)
GROUP BY k.id, k.ho_ten
HAVING COUNT(DISTINCT c.san_pham_id) = 2;
""",
        "sai": [
            "SELECT DISTINCT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id"
            " JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE c.san_pham_id IN (1, 2)",
            "SELECT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id"
            " JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE c.san_pham_id IN (1, 2)"
            " GROUP BY k.id, k.ho_ten HAVING COUNT(*) = 2",
        ],
    },
    {
        "id": "du-an-phong-khac",
        "ten": "Nhân viên làm dự án của phòng khác",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien", "phan_cong", "du_an"],
        "de": """
Liệt kê **mã nhân viên, họ tên, tên dự án** cho mỗi lần một nhân viên được phân công vào dự án **không thuộc phòng
của mình**. Chỉ xét nhân viên **đã xếp phòng** và dự án **đã giao cho một phòng**.

Nhớ: so sánh `<>` với NULL ra *không biết*, dòng đó bị loại — đúng ý đề. `IS NOT` thì coi NULL như một giá trị.
""",
        "them_du_lieu": """
INSERT INTO phan_cong VALUES
    (8, 1, 30),
    (5, 2, 20);
""",
        "dap_an": """
SELECT n.id, n.ho_ten, d.ten
FROM nhan_vien n
JOIN phan_cong p ON p.nhan_vien_id = n.id
JOIN du_an d ON d.id = p.du_an_id
WHERE d.phong_id <> n.phong_id;
""",
        "sai": [
            "SELECT n.id, n.ho_ten, d.ten FROM nhan_vien n JOIN phan_cong p ON p.nhan_vien_id = n.id"
            " JOIN du_an d ON d.id = p.du_an_id WHERE d.phong_id <> n.phong_id OR d.phong_id IS NULL",
            "SELECT n.id, n.ho_ten, d.ten FROM nhan_vien n JOIN du_an d ON d.phong_id <> n.phong_id",
        ],
    },
    {
        "id": "diem-cao-nhat-lan",
        "ten": "Điểm lần thi cao nhất mỗi sinh viên mỗi môn",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua", "sinh_vien", "mon_hoc"],
        "de": """
Với mỗi sinh viên và mỗi môn sinh viên đó đã thi, cho biết **mã sinh viên, họ tên, tên môn, điểm cao nhất** trong
các lần thi. Lần thi vắng (NULL) không tính điểm; nếu mọi lần đều vắng thì điểm cao nhất là NULL.

Gộp nhóm theo **mã** sinh viên, không theo họ tên: hai sinh viên có thể trùng tên.
""",
        "them_du_lieu": """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 4.5),
    (1, 2, 2, 6.5);
""",
        "dap_an": """
SELECT k.sinh_vien_id, s.ho_ten, m.ten_mon, MAX(k.diem) AS diem_cao_nhat
FROM ket_qua k
JOIN sinh_vien s ON s.id = k.sinh_vien_id
JOIN mon_hoc m ON m.id = k.mon_hoc_id
GROUP BY k.sinh_vien_id, s.ho_ten, k.mon_hoc_id, m.ten_mon;
""",
        "sai": [
            "SELECT MIN(k.sinh_vien_id), s.ho_ten, m.ten_mon, MAX(k.diem) FROM ket_qua k"
            " JOIN sinh_vien s ON s.id = k.sinh_vien_id JOIN mon_hoc m ON m.id = k.mon_hoc_id"
            " GROUP BY s.ho_ten, m.ten_mon",
            "SELECT k.sinh_vien_id, s.ho_ten, m.ten_mon, k.diem FROM ket_qua k"
            " JOIN sinh_vien s ON s.id = k.sinh_vien_id JOIN mon_hoc m ON m.id = k.mon_hoc_id"
            " WHERE k.lan_thi = 1",
        ],
    },
]
