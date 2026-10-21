"""sql.tong-hop — bài tổng hợp nhiều mệnh đề, CSDL ban-hang và nhan-su (docs/plans/danh-muc-bai.md)."""

BAI = [
    {
        "id": "top-khach",
        "ten": "3 khách chi nhiều nhất năm 2024",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "thu_tu": True,
        "bang": ["khach_hang", "don_hang", "chi_tiet_don"],
        "de": """
Liệt kê **mã, họ tên và tổng tiền đã chi** của **3 khách chi nhiều nhất trong năm 2024**, khách chi nhiều nhất
đứng đầu. Cùng tổng tiền thì khách có **mã nhỏ hơn đứng trước**.

Tổng tiền của khách là tổng `so_luong * don_gia` trên mọi chi tiết thuộc các đơn của khách đặt trong năm 2024,
**không tính đơn đã huỷ** (`trang_thai = 'huy'`).
""",
        "dap_an": """
SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) AS tong_tien
FROM khach_hang k
JOIN don_hang d ON d.khach_hang_id = k.id
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai <> 'huy' AND d.ngay_dat >= '2024-01-01' AND d.ngay_dat < '2025-01-01'
GROUP BY k.id, k.ho_ten
ORDER BY tong_tien DESC, k.id
LIMIT 3;
""",
        "sai": [
            "SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) AS tong_tien FROM khach_hang k"
            " JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " GROUP BY k.id, k.ho_ten ORDER BY tong_tien DESC, k.id LIMIT 3",
            "SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) AS tong_tien FROM khach_hang k"
            " JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten ORDER BY tong_tien, k.id LIMIT 3",
        ],
    },
    {
        "id": "doanh-thu-thanh-pho",
        "ten": "Doanh thu theo thành phố",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "thu_tu": True,
        "bang": ["khach_hang", "don_hang", "chi_tiet_don"],
        "de": """
Tính **doanh thu của từng thành phố**: tổng `so_luong * don_gia` của các đơn **đã giao** (`da-giao`) mà khách đặt
đơn ở thành phố đó. Bỏ đơn của khách vãng lai và của khách chưa ghi thành phố.

Sắp theo doanh thu **giảm dần**; bằng nhau thì theo **tên thành phố** tăng dần.
""",
        "them_du_lieu": """
INSERT INTO don_hang VALUES
    (11, 4, '2024-09-28', 'da-giao');

INSERT INTO chi_tiet_don VALUES
    (11, 3, 1, 5000000);
""",
        "dap_an": """
SELECT k.thanh_pho, SUM(c.so_luong * c.don_gia) AS doanh_thu
FROM don_hang d
JOIN khach_hang k ON k.id = d.khach_hang_id
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai = 'da-giao' AND k.thanh_pho IS NOT NULL
GROUP BY k.thanh_pho
ORDER BY doanh_thu DESC, k.thanh_pho;
""",
        "sai": [
            "SELECT k.thanh_pho, SUM(c.so_luong * c.don_gia) AS doanh_thu FROM don_hang d"
            " JOIN khach_hang k ON k.id = d.khach_hang_id JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " WHERE k.thanh_pho IS NOT NULL GROUP BY k.thanh_pho ORDER BY doanh_thu DESC, k.thanh_pho",
            "SELECT k.thanh_pho, SUM(c.so_luong * c.don_gia) AS doanh_thu FROM don_hang d"
            " JOIN khach_hang k ON k.id = d.khach_hang_id JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " WHERE d.trang_thai = 'da-giao' GROUP BY k.thanh_pho ORDER BY doanh_thu DESC, k.thanh_pho",
        ],
    },
    {
        "id": "ban-chay-danh-muc",
        "ten": "Sản phẩm bán chạy nhất trong từng danh mục",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham", "don_hang", "chi_tiet_don"],
        "de": """
Với mỗi danh mục (`san_pham.danh_muc_id`), tìm **sản phẩm bán chạy nhất**: tổng `so_luong` bán ra lớn nhất, chỉ
tính các đơn **không bị huỷ**. Nhiều sản phẩm cùng bằng mức cao nhất của danh mục thì **in tất cả**. Danh mục chưa
bán được gì thì không có dòng nào.

Kết quả: mã danh mục, mã sản phẩm, tên sản phẩm, tổng số lượng.

Gợi ý: gộp theo sản phẩm, rồi giữ nhóm có tổng bằng tổng lớn nhất **của chính danh mục đó** (truy vấn con tương
quan trong `HAVING`; truy vấn con có thể dùng `GROUP BY … ORDER BY … LIMIT 1`).
""",
        "dap_an": """
SELECT s.danh_muc_id, s.id, s.ten, SUM(c.so_luong) AS so_luong
FROM san_pham s
JOIN chi_tiet_don c ON c.san_pham_id = s.id
JOIN don_hang d ON d.id = c.don_hang_id
WHERE d.trang_thai <> 'huy'
GROUP BY s.danh_muc_id, s.id, s.ten
HAVING SUM(c.so_luong) = (
    SELECT SUM(c2.so_luong)
    FROM san_pham s2
    JOIN chi_tiet_don c2 ON c2.san_pham_id = s2.id
    JOIN don_hang d2 ON d2.id = c2.don_hang_id
    WHERE s2.danh_muc_id = s.danh_muc_id AND d2.trang_thai <> 'huy'
    GROUP BY s2.id
    ORDER BY 1 DESC
    LIMIT 1
);
""",
        "sai": [
            "SELECT s.danh_muc_id, s.id, s.ten, SUM(c.so_luong) AS so_luong FROM san_pham s"
            " JOIN chi_tiet_don c ON c.san_pham_id = s.id GROUP BY s.danh_muc_id, s.id, s.ten"
            " HAVING SUM(c.so_luong) = (SELECT SUM(c2.so_luong) FROM san_pham s2"
            " JOIN chi_tiet_don c2 ON c2.san_pham_id = s2.id WHERE s2.danh_muc_id = s.danh_muc_id"
            " GROUP BY s2.id ORDER BY 1 DESC LIMIT 1)",
            "SELECT s.danh_muc_id, s.id, s.ten, COUNT(*) AS so_luong FROM san_pham s"
            " JOIN chi_tiet_don c ON c.san_pham_id = s.id JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE d.trang_thai <> 'huy' GROUP BY s.danh_muc_id, s.id, s.ten"
            " HAVING COUNT(*) = (SELECT COUNT(*) FROM san_pham s2 JOIN chi_tiet_don c2 ON c2.san_pham_id = s2.id"
            " JOIN don_hang d2 ON d2.id = c2.don_hang_id WHERE s2.danh_muc_id = s.danh_muc_id"
            " AND d2.trang_thai <> 'huy' GROUP BY s2.id ORDER BY 1 DESC LIMIT 1)",
        ],
    },
    {
        "id": "mua-lai",
        "ten": "Khách đặt đơn trong cả quý 1 và quý 2",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang"],
        "de": """
Liệt kê **mã và họ tên** các khách có đặt đơn trong **quý 1/2024** (01/01 đến hết 31/03) **và** cũng có đặt đơn
trong **quý 2/2024** (01/04 đến hết 30/06). Tính mọi đơn, kể cả đơn đã huỷ.

Chú ý các đơn đặt đúng ngày đầu hoặc cuối quý.
""",
        "them_du_lieu": """
INSERT INTO don_hang VALUES
    (11, 1, '2024-02-10', 'da-giao'),
    (12, 1, '2024-05-03', 'da-giao'),
    (13, 2, '2024-03-31', 'da-giao'),
    (14, 3, '2024-04-01', 'huy'),
    (15, 3, '2024-01-20', 'moi'),
    (16, 2, '2024-07-01', 'da-giao');
""",
        "dap_an": """
SELECT id, ho_ten
FROM khach_hang
WHERE id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-01-01' AND '2024-03-31')
  AND id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-04-01' AND '2024-06-30');
""",
        "sai": [
            "SELECT id, ho_ten FROM khach_hang"
            " WHERE id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-01-01' AND '2024-03-31')"
            " OR id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-04-01' AND '2024-06-30')",
            "SELECT id, ho_ten FROM khach_hang"
            " WHERE id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-01-01' AND '2024-03-31')"
            " AND id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat > '2024-04-01' AND ngay_dat <= '2024-06-30')",
        ],
    },
    {
        "id": "ty-le-huy",
        "ten": "Tỉ lệ đơn huỷ theo tháng",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang"],
        "de": """
Với mỗi tháng có đơn hàng, tính **tỉ lệ phần trăm đơn bị huỷ** trong số đơn đặt trong tháng đó, **làm tròn 2 chữ
số** sau dấu phẩy. Tháng ghi dạng `YYYY-MM`: `strftime('%Y-%m', ngay_dat)`.

Cẩn thận phép chia số nguyên: trong SQLite `1 / 4` ra `0`. Nhân với `100.0` trước khi chia.
""",
        "cot": ["thang", "ty_le_huy"],
        "dap_an": """
SELECT strftime('%Y-%m', ngay_dat) AS thang,
       ROUND(100.0 * SUM(CASE WHEN trang_thai = 'huy' THEN 1 ELSE 0 END) / COUNT(*), 2) AS ty_le_huy
FROM don_hang
GROUP BY strftime('%Y-%m', ngay_dat);
""",
        "sai": [
            "SELECT strftime('%Y-%m', ngay_dat) AS thang,"
            " SUM(CASE WHEN trang_thai = 'huy' THEN 1 ELSE 0 END) / COUNT(*) * 100 AS ty_le_huy"
            " FROM don_hang GROUP BY strftime('%Y-%m', ngay_dat)",
            "SELECT strftime('%Y-%m', ngay_dat) AS thang,"
            " ROUND(100.0 * COUNT(trang_thai = 'huy') / COUNT(*), 2) AS ty_le_huy"
            " FROM don_hang GROUP BY strftime('%Y-%m', ngay_dat)",
        ],
    },
    {
        "id": "don-tb-khach",
        "ten": "Giá trị đơn trung bình của khách có từ 2 đơn",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang", "chi_tiet_don"],
        "de": """
Với mỗi khách có **từ 2 đơn trở lên** (không tính đơn huỷ), tính **giá trị trung bình một đơn**, làm tròn 2 chữ số.

Giá trị một đơn là tổng `so_luong * don_gia` các dòng chi tiết của đơn. Trung bình một đơn = tổng tiền của khách
chia cho **số đơn**, không phải trung bình của các dòng chi tiết (một đơn có thể có nhiều dòng).

Kết quả: mã khách, họ tên, giá trị trung bình.
""",
        "dap_an": """
SELECT k.id, k.ho_ten,
       ROUND(1.0 * SUM(c.so_luong * c.don_gia) / COUNT(DISTINCT d.id), 2) AS gia_tri_tb
FROM khach_hang k
JOIN don_hang d ON d.khach_hang_id = k.id
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai <> 'huy'
GROUP BY k.id, k.ho_ten
HAVING COUNT(DISTINCT d.id) >= 2;
""",
        "sai": [
            "SELECT k.id, k.ho_ten, ROUND(AVG(c.so_luong * c.don_gia), 2) AS gia_tri_tb FROM khach_hang k"
            " JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten HAVING COUNT(DISTINCT d.id) >= 2",
            "SELECT k.id, k.ho_ten, ROUND(1.0 * SUM(c.so_luong * c.don_gia) / COUNT(DISTINCT d.id), 2) AS gia_tri_tb"
            " FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id"
            " WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten HAVING COUNT(*) >= 2",
        ],
    },
    {
        "id": "ton-lau",
        "ten": "Sản phẩm còn tồn mà 90 ngày chưa bán",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham", "don_hang", "chi_tiet_don"],
        "de": """
Hôm nay là **31/12/2024**. Liệt kê **mã, tên, tồn kho** các sản phẩm **còn hàng** (`ton_kho > 0`) nhưng **không
bán được** trong 90 ngày gần đây: không có đơn nào (trừ đơn huỷ) đặt **từ ngày 02/10/2024 trở đi** có sản phẩm đó.

Ngày mốc tính được bằng `date('2024-12-31', '-90 days')`, ra `'2024-10-02'`. Đơn đặt đúng ngày 02/10 vẫn tính là
đã bán.
""",
        "them_du_lieu": """
INSERT INTO don_hang VALUES
    (11, 5, '2024-11-15', 'da-giao'),
    (12, 6, '2024-12-01', 'huy'),
    (13, 2, '2024-10-02', 'moi');

INSERT INTO chi_tiet_don VALUES
    (11, 3, 1, 5000000),
    (12, 7, 1, 150000),
    (13, 9, 1, 16000000);
""",
        "dap_an": """
SELECT s.id, s.ten, s.ton_kho
FROM san_pham s
WHERE s.ton_kho > 0
  AND NOT EXISTS (
      SELECT 1
      FROM chi_tiet_don c
      JOIN don_hang d ON d.id = c.don_hang_id
      WHERE c.san_pham_id = s.id AND d.trang_thai <> 'huy' AND d.ngay_dat >= date('2024-12-31', '-90 days')
  );
""",
        "sai": [
            "SELECT s.id, s.ten, s.ton_kho FROM san_pham s WHERE s.ton_kho > 0 AND NOT EXISTS (SELECT 1"
            " FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = s.id"
            " AND d.ngay_dat >= '2024-10-02')",
            "SELECT s.id, s.ten, s.ton_kho FROM san_pham s WHERE s.ton_kho > 0 AND NOT EXISTS (SELECT 1"
            " FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = s.id"
            " AND d.trang_thai <> 'huy' AND d.ngay_dat > '2024-10-02')",
            "SELECT s.id, s.ten, s.ton_kho FROM san_pham s WHERE NOT EXISTS (SELECT 1"
            " FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = s.id"
            " AND d.trang_thai <> 'huy' AND d.ngay_dat >= '2024-10-02')",
        ],
    },
    {
        "id": "danh-muc-e",
        "ten": "Danh mục không bán được sản phẩm nào năm 2024",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["danh_muc", "san_pham", "don_hang", "chi_tiet_don"],
        "de": """
Liệt kê **mã và tên** các danh mục **không bán được sản phẩm nào trong năm 2024**: không có sản phẩm nào thuộc
**trực tiếp** danh mục đó (`san_pham.danh_muc_id`) nằm trong một đơn **không bị huỷ** đặt trong năm 2024.

Danh mục không có sản phẩm nào cũng tính là không bán được. Sản phẩm chỉ nằm trong đơn đã huỷ thì chưa bán được.
""",
        "them_du_lieu": """
INSERT INTO danh_muc VALUES
    (6, 'Phụ kiện máy tính', 1);

INSERT INTO san_pham VALUES
    (11, 'Bàn phím cơ', 6, 1200000, 10);

INSERT INTO don_hang VALUES
    (11, 6, '2024-09-25', 'huy');

INSERT INTO chi_tiet_don VALUES
    (11, 11, 1, 1200000);
""",
        "dap_an": """
SELECT dm.id, dm.ten
FROM danh_muc dm
WHERE NOT EXISTS (
    SELECT 1
    FROM san_pham s
    JOIN chi_tiet_don c ON c.san_pham_id = s.id
    JOIN don_hang d ON d.id = c.don_hang_id
    WHERE s.danh_muc_id = dm.id AND d.trang_thai <> 'huy'
      AND d.ngay_dat >= '2024-01-01' AND d.ngay_dat < '2025-01-01'
);
""",
        "sai": [
            "SELECT dm.id, dm.ten FROM danh_muc dm WHERE NOT EXISTS (SELECT 1 FROM san_pham s"
            " JOIN chi_tiet_don c ON c.san_pham_id = s.id JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE s.danh_muc_id = dm.id AND d.ngay_dat >= '2024-01-01' AND d.ngay_dat < '2025-01-01')",
            "SELECT dm.id, dm.ten FROM danh_muc dm LEFT JOIN san_pham s ON s.danh_muc_id = dm.id WHERE s.id IS NULL",
        ],
    },
    {
        "id": "don-lon-nhat",
        "ten": "Đơn giá trị lớn nhất của mỗi khách",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang", "chi_tiet_don"],
        "de": """
Với mỗi khách (bỏ khách vãng lai), tìm **đơn có giá trị lớn nhất** trong các đơn **không bị huỷ** của khách đó.
Giá trị đơn là tổng `so_luong * don_gia` các dòng chi tiết. Nhiều đơn cùng bằng giá trị lớn nhất thì **in tất cả**.

Kết quả: mã khách, mã đơn, giá trị đơn.
""",
        "them_du_lieu": """
INSERT INTO don_hang VALUES
    (11, 3, '2024-10-05', 'da-giao');

INSERT INTO chi_tiet_don VALUES
    (11, 3, 2, 5000000);
""",
        "dap_an": """
SELECT d.khach_hang_id, d.id, SUM(c.so_luong * c.don_gia) AS gia_tri
FROM don_hang d
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.khach_hang_id IS NOT NULL AND d.trang_thai <> 'huy'
GROUP BY d.khach_hang_id, d.id
HAVING SUM(c.so_luong * c.don_gia) = (
    SELECT SUM(c2.so_luong * c2.don_gia)
    FROM don_hang d2
    JOIN chi_tiet_don c2 ON c2.don_hang_id = d2.id
    WHERE d2.khach_hang_id = d.khach_hang_id AND d2.trang_thai <> 'huy'
    GROUP BY d2.id
    ORDER BY 1 DESC
    LIMIT 1
);
""",
        "sai": [
            "SELECT d.khach_hang_id, d.id, SUM(c.so_luong * c.don_gia) AS gia_tri FROM don_hang d"
            " JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.khach_hang_id IS NOT NULL"
            " GROUP BY d.khach_hang_id, d.id HAVING SUM(c.so_luong * c.don_gia) = (SELECT SUM(c2.so_luong * c2.don_gia)"
            " FROM don_hang d2 JOIN chi_tiet_don c2 ON c2.don_hang_id = d2.id WHERE d2.khach_hang_id = d.khach_hang_id"
            " GROUP BY d2.id ORDER BY 1 DESC LIMIT 1)",
            "SELECT d.khach_hang_id, d.id, SUM(c.so_luong * c.don_gia) AS gia_tri FROM don_hang d"
            " JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.khach_hang_id IS NOT NULL AND d.trang_thai <> 'huy'"
            " GROUP BY d.khach_hang_id, d.id HAVING d.id = (SELECT d2.id FROM don_hang d2"
            " JOIN chi_tiet_don c2 ON c2.don_hang_id = d2.id WHERE d2.khach_hang_id = d.khach_hang_id"
            " AND d2.trang_thai <> 'huy' GROUP BY d2.id ORDER BY SUM(c2.so_luong * c2.don_gia) DESC LIMIT 1)",
        ],
    },
    {
        "id": "khach-moi",
        "ten": "Khách mới theo tháng và số người đã mua",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang"],
        "de": """
Với mỗi tháng có khách đăng ký (tháng dạng `YYYY-MM` của `ngay_dang_ky`), cho biết **số khách mới** đăng ký trong
tháng và **số người trong đó đã mua**: có ít nhất một đơn **không bị huỷ**.

Tháng có khách đăng ký mà chưa ai mua vẫn phải có dòng, với số người đã mua bằng 0. Điều kiện về đơn đặt ở `WHERE`
hay ở `ON` của `LEFT JOIN` cho kết quả khác nhau.
""",
        "cot": ["thang", "khach_moi", "da_mua"],
        "dap_an": """
SELECT strftime('%Y-%m', k.ngay_dang_ky) AS thang,
       COUNT(DISTINCT k.id) AS khach_moi,
       COUNT(DISTINCT d.khach_hang_id) AS da_mua
FROM khach_hang k
LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy'
GROUP BY strftime('%Y-%m', k.ngay_dang_ky);
""",
        "sai": [
            "SELECT strftime('%Y-%m', k.ngay_dang_ky) AS thang, COUNT(DISTINCT k.id), COUNT(DISTINCT d.khach_hang_id)"
            " FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id WHERE d.trang_thai <> 'huy'"
            " GROUP BY strftime('%Y-%m', k.ngay_dang_ky)",
            "SELECT strftime('%Y-%m', k.ngay_dang_ky) AS thang, COUNT(*), COUNT(d.khach_hang_id)"
            " FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy'"
            " GROUP BY strftime('%Y-%m', k.ngay_dang_ky)",
        ],
    },
    {
        "id": "dong-bo-ton-kho",
        "ten": "Trừ tồn kho theo các đơn đã giao trong ngày 20/09/2024",
        "loai": "luyen-tap",
        "loai_cau": "dml",
        "csdl": "ban-hang",
        "bang": ["san_pham", "don_hang", "chi_tiet_don"],
        "de": """
Cuối ngày **20/09/2024**, kho cập nhật tồn kho: với mỗi sản phẩm, **trừ `ton_kho`** đi **tổng số lượng** của sản
phẩm đó trong mọi đơn **đã giao** (`da-giao`) đặt ngày `'2024-09-20'`. Sản phẩm không nằm trong đơn nào như vậy
giữ nguyên tồn kho. Đơn khác trạng thái không tính.

Một sản phẩm có thể nằm trong nhiều đơn của ngày đó: phải trừ **tổng** số lượng. Dùng truy vấn con tương quan trong
`SET` (tham chiếu `san_pham.id`), và `WHERE` để chỉ đổi những sản phẩm có trong các đơn đó.
""",
        "them_du_lieu": """
INSERT INTO don_hang VALUES
    (11, 3, '2024-09-20', 'da-giao'),
    (12, 5, '2024-09-20', 'huy');

INSERT INTO chi_tiet_don VALUES
    (11, 8, 3, 180000),
    (11, 1, 1, 22000000),
    (12, 2, 1, 20000000);
""",
        "dap_an": """
UPDATE san_pham
SET ton_kho = ton_kho - (
    SELECT SUM(c.so_luong)
    FROM chi_tiet_don c
    JOIN don_hang d ON d.id = c.don_hang_id
    WHERE c.san_pham_id = san_pham.id AND d.ngay_dat = '2024-09-20' AND d.trang_thai = 'da-giao'
)
WHERE id IN (
    SELECT c.san_pham_id
    FROM chi_tiet_don c
    JOIN don_hang d ON d.id = c.don_hang_id
    WHERE d.ngay_dat = '2024-09-20' AND d.trang_thai = 'da-giao'
);
""",
        "sai": [
            "UPDATE san_pham SET ton_kho = ton_kho - (SELECT SUM(c.so_luong) FROM chi_tiet_don c"
            " JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = san_pham.id AND d.ngay_dat = '2024-09-20')"
            " WHERE id IN (SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE d.ngay_dat = '2024-09-20')",
            "UPDATE san_pham SET ton_kho = ton_kho - (SELECT c.so_luong FROM chi_tiet_don c"
            " JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = san_pham.id AND d.ngay_dat = '2024-09-20'"
            " AND d.trang_thai = 'da-giao') WHERE id IN (SELECT c.san_pham_id FROM chi_tiet_don c"
            " JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat = '2024-09-20' AND d.trang_thai = 'da-giao')",
        ],
    },
    {
        "id": "don-dep-khach",
        "ten": "Xoá khách không có đơn, đăng ký trước 01/07/2024",
        "loai": "luyen-tap",
        "loai_cau": "dml",
        "csdl": "ban-hang",
        "bang": ["khach_hang", "don_hang"],
        "de": """
Xoá khỏi bảng `khach_hang` những khách **chưa từng đặt đơn nào** (kể cả đơn huỷ) và **đăng ký trước ngày
01/07/2024**.

Cẩn thận với `NOT IN (truy vấn con)` khi truy vấn con có giá trị NULL: đơn của khách vãng lai có
`khach_hang_id` NULL.
""",
        "dap_an": """
DELETE FROM khach_hang
WHERE ngay_dang_ky < '2024-07-01'
  AND id NOT IN (SELECT khach_hang_id FROM don_hang WHERE khach_hang_id IS NOT NULL);
""",
        "sai": [
            "DELETE FROM khach_hang WHERE ngay_dang_ky < '2024-07-01'"
            " AND id NOT IN (SELECT khach_hang_id FROM don_hang)",
            "DELETE FROM khach_hang WHERE id NOT IN (SELECT khach_hang_id FROM don_hang WHERE khach_hang_id IS NOT NULL)",
        ],
    },
    {
        "id": "tren-tb-phong",
        "ten": "Nhân viên lương trên trung bình phòng mình",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mã, họ tên, mã phòng** và **mức chênh** (lương trừ lương trung bình của phòng) của những nhân viên có
lương **cao hơn lương trung bình của chính phòng mình**. Nhân viên chưa xếp phòng không xét.

So với trung bình **phòng mình**, không phải trung bình cả công ty: dùng truy vấn con tương quan.
""",
        "cot": ["id", "ho_ten", "phong_id", "chenh"],
        "dap_an": """
SELECT nv.id, nv.ho_ten, nv.phong_id, nv.luong - AVG(n2.luong) AS chenh
FROM nhan_vien nv
JOIN nhan_vien n2 ON n2.phong_id = nv.phong_id
GROUP BY nv.id, nv.ho_ten, nv.phong_id, nv.luong
HAVING nv.luong > AVG(n2.luong);
""",
        "sai": [
            "SELECT nv.id, nv.ho_ten, nv.phong_id, nv.luong - (SELECT AVG(luong) FROM nhan_vien) AS chenh"
            " FROM nhan_vien nv WHERE nv.luong > (SELECT AVG(luong) FROM nhan_vien)",
            "SELECT nv.id, nv.ho_ten, nv.phong_id,"
            " nv.luong - (SELECT AVG(n2.luong) FROM nhan_vien n2 WHERE n2.phong_id = nv.phong_id) AS chenh"
            " FROM nhan_vien nv WHERE nv.luong >= (SELECT AVG(n2.luong) FROM nhan_vien n2 WHERE n2.phong_id = nv.phong_id)",
        ],
    },
    {
        "id": "quan-ly-cap-hai",
        "ten": "Quản lý cấp hai ở phòng khác",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
"Quản lý cấp hai" của một nhân viên là quản lý của người quản lý trực tiếp. Liệt kê **mã và họ tên** các nhân viên
có quản lý cấp hai **thuộc phòng khác** phòng của nhân viên. Chỉ xét khi cả hai người đều đã xếp phòng.

Cần nối bảng `nhan_vien` với chính nó **hai lần**.
""",
        "dap_an": """
SELECT nv.id, nv.ho_ten
FROM nhan_vien nv
JOIN nhan_vien ql ON ql.id = nv.quan_ly_id
JOIN nhan_vien ql2 ON ql2.id = ql.quan_ly_id
WHERE ql2.phong_id <> nv.phong_id;
""",
        "sai": [
            "SELECT nv.id, nv.ho_ten FROM nhan_vien nv JOIN nhan_vien ql ON ql.id = nv.quan_ly_id"
            " WHERE ql.phong_id <> nv.phong_id",
            "SELECT nv.id, nv.ho_ten FROM nhan_vien nv JOIN nhan_vien ql ON ql.id = nv.quan_ly_id"
            " JOIN nhan_vien ql2 ON ql2.id = ql.quan_ly_id WHERE ql2.phong_id <> ql.phong_id",
        ],
    },
    {
        "id": "du-an-lien-phong",
        "ten": "Dự án có nhiều nhân viên từ phòng khác nhất",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["du_an", "phan_cong", "nhan_vien"],
        "de": """
Một nhân viên được phân công vào dự án là "từ phòng khác" nếu phòng của nhân viên khác phòng phụ trách dự án.
Tìm **dự án có nhiều nhân viên từ phòng khác nhất**; bằng nhau thì **in tất cả**. Chỉ xét dự án đã giao phòng và
nhân viên đã xếp phòng.

Kết quả: mã dự án, tên dự án, số nhân viên từ phòng khác.
""",
        "them_du_lieu": """
INSERT INTO phan_cong VALUES
    (8, 1, 40),
    (5, 2, 30),
    (6, 1, 20),
    (2, 3, 10),
    (8, 3, 15);
""",
        "dap_an": """
SELECT da.id, da.ten, COUNT(DISTINCT pc.nhan_vien_id) AS so_nv
FROM du_an da
JOIN phan_cong pc ON pc.du_an_id = da.id
JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id
WHERE nv.phong_id <> da.phong_id
GROUP BY da.id, da.ten
HAVING COUNT(DISTINCT pc.nhan_vien_id) = (
    SELECT COUNT(DISTINCT pc2.nhan_vien_id)
    FROM du_an d2
    JOIN phan_cong pc2 ON pc2.du_an_id = d2.id
    JOIN nhan_vien n2 ON n2.id = pc2.nhan_vien_id
    WHERE n2.phong_id <> d2.phong_id
    GROUP BY d2.id
    ORDER BY 1 DESC
    LIMIT 1
);
""",
        "sai": [
            "SELECT da.id, da.ten, COUNT(DISTINCT pc.nhan_vien_id) AS so_nv FROM du_an da"
            " JOIN phan_cong pc ON pc.du_an_id = da.id JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id"
            " WHERE nv.phong_id <> da.phong_id GROUP BY da.id, da.ten ORDER BY so_nv DESC LIMIT 1",
            "SELECT da.id, da.ten, COUNT(*) AS so_nv FROM du_an da JOIN phan_cong pc ON pc.du_an_id = da.id"
            " GROUP BY da.id, da.ten HAVING COUNT(*) = (SELECT COUNT(*) FROM phan_cong GROUP BY du_an_id"
            " ORDER BY 1 DESC LIMIT 1)",
        ],
    },
    {
        "id": "gio-theo-phong",
        "ten": "Tổng giờ dự án theo phòng",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["phong_ban", "du_an", "phan_cong"],
        "de": """
Với **mọi phòng**, tính **tổng số giờ** đã phân công trên các dự án mà phòng đó **phụ trách** (`du_an.phong_id`).
Phòng không phụ trách dự án nào, hoặc dự án chưa ai làm, có tổng giờ là **0** (không phải NULL).

Kết quả: mã phòng, tên phòng, tổng giờ.
""",
        "dap_an": """
SELECT pb.id, pb.ten, COALESCE(SUM(pc.so_gio), 0) AS tong_gio
FROM phong_ban pb
LEFT JOIN du_an da ON da.phong_id = pb.id
LEFT JOIN phan_cong pc ON pc.du_an_id = da.id
GROUP BY pb.id, pb.ten;
""",
        "sai": [
            "SELECT pb.id, pb.ten, SUM(pc.so_gio) AS tong_gio FROM phong_ban pb"
            " JOIN du_an da ON da.phong_id = pb.id JOIN phan_cong pc ON pc.du_an_id = da.id GROUP BY pb.id, pb.ten",
            "SELECT pb.id, pb.ten, SUM(pc.so_gio) AS tong_gio FROM phong_ban pb"
            " LEFT JOIN du_an da ON da.phong_id = pb.id LEFT JOIN phan_cong pc ON pc.du_an_id = da.id"
            " GROUP BY pb.id, pb.ten",
        ],
    },
    {
        "id": "moi-du-an-phong",
        "ten": "Nhân viên tham gia mọi dự án của phòng mình",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien", "du_an", "phan_cong"],
        "de": """
Liệt kê **mã và họ tên** các nhân viên được phân công vào **mọi dự án** mà phòng của mình phụ trách. Chỉ xét
nhân viên mà phòng có **ít nhất một** dự án (phòng không có dự án thì "mọi dự án" đúng một cách vô nghĩa, không
tính).

Đây là phép chia: "không có dự án nào của phòng mà nhân viên **không** tham gia" — hai `NOT EXISTS` lồng nhau.
""",
        "dap_an": """
SELECT nv.id, nv.ho_ten
FROM nhan_vien nv
WHERE EXISTS (SELECT 1 FROM du_an da WHERE da.phong_id = nv.phong_id)
  AND NOT EXISTS (
      SELECT 1
      FROM du_an da
      WHERE da.phong_id = nv.phong_id
        AND NOT EXISTS (
            SELECT 1 FROM phan_cong pc WHERE pc.du_an_id = da.id AND pc.nhan_vien_id = nv.id
        )
  );
""",
        "sai": [
            "SELECT nv.id, nv.ho_ten FROM nhan_vien nv WHERE NOT EXISTS (SELECT 1 FROM du_an da"
            " WHERE da.phong_id = nv.phong_id AND NOT EXISTS (SELECT 1 FROM phan_cong pc"
            " WHERE pc.du_an_id = da.id AND pc.nhan_vien_id = nv.id))",
            "SELECT DISTINCT nv.id, nv.ho_ten FROM nhan_vien nv JOIN phan_cong pc ON pc.nhan_vien_id = nv.id"
            " JOIN du_an da ON da.id = pc.du_an_id WHERE da.phong_id = nv.phong_id",
        ],
    },
    {
        "id": "tham-nien",
        "ten": "Số nhân viên và lương trung bình theo năm vào làm",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Với mỗi **năm vào làm** (`strftime('%Y', ngay_vao_lam)`), cho biết **số nhân viên** và **lương trung bình** của
họ, làm tròn 2 chữ số.
""",
        "cot": ["nam", "so_nv", "luong_tb"],
        "dap_an": """
SELECT strftime('%Y', ngay_vao_lam) AS nam, COUNT(*) AS so_nv, ROUND(AVG(luong), 2) AS luong_tb
FROM nhan_vien
GROUP BY strftime('%Y', ngay_vao_lam);
""",
        "sai": [
            "SELECT ngay_vao_lam, COUNT(*), ROUND(AVG(luong), 2) FROM nhan_vien GROUP BY ngay_vao_lam",
            "SELECT strftime('%Y', ngay_vao_lam) AS nam, COUNT(*), SUM(luong) FROM nhan_vien"
            " GROUP BY strftime('%Y', ngay_vao_lam)",
        ],
    },
    {
        "id": "chi-phi-du-an",
        "ten": "Chi phí nhân công ước tính mỗi dự án",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["du_an", "phan_cong", "nhan_vien"],
        "de": """
Ước tính **chi phí nhân công** của **mọi dự án**: mỗi nhân viên được phân công tốn `so_gio * luong / 160` (lương
tháng chia 160 giờ làm). Dự án chưa ai làm có chi phí **0**.

Kết quả: mã dự án, tên dự án, chi phí.
""",
        "them_du_lieu": """
INSERT INTO du_an VALUES
    (5, 'Kho dữ liệu', 1, 300000000);
""",
        "dap_an": """
SELECT da.id, da.ten, COALESCE(SUM(pc.so_gio * nv.luong / 160.0), 0) AS chi_phi
FROM du_an da
LEFT JOIN phan_cong pc ON pc.du_an_id = da.id
LEFT JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id
GROUP BY da.id, da.ten;
""",
        "sai": [
            "SELECT da.id, da.ten, SUM(pc.so_gio * nv.luong / 160.0) AS chi_phi FROM du_an da"
            " JOIN phan_cong pc ON pc.du_an_id = da.id JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id"
            " GROUP BY da.id, da.ten",
            "SELECT da.id, da.ten, COALESCE(SUM(pc.so_gio * nv.luong), 0) AS chi_phi FROM du_an da"
            " LEFT JOIN phan_cong pc ON pc.du_an_id = da.id LEFT JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id"
            " GROUP BY da.id, da.ten",
        ],
    },
    {
        "id": "phong-vuot-luong",
        "ten": "Phòng có tổng lương cả năm vượt ngân sách",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["phong_ban", "nhan_vien"],
        "de": """
Liệt kê **mã, tên phòng, tổng lương cả năm** (tổng lương tháng của nhân viên trong phòng nhân 12) và **ngân sách**
của những phòng có tổng lương cả năm **vượt** ngân sách phòng.

`luong` là lương **một tháng**.
""",
        "them_du_lieu": """
INSERT INTO phong_ban VALUES
    (5, 'Tài chính', 900000000);

INSERT INTO nhan_vien VALUES
    (11, 'Lý Thị Vân', 5, 1, 30000000, '2016-02-01'),
    (12, 'Mai Văn Xuân', 5, 11, 20000000, '2019-03-03');
""",
        "dap_an": """
SELECT pb.id, pb.ten, SUM(nv.luong) * 12 AS tong_luong_nam, pb.ngan_sach
FROM phong_ban pb
JOIN nhan_vien nv ON nv.phong_id = pb.id
GROUP BY pb.id, pb.ten, pb.ngan_sach
HAVING SUM(nv.luong) * 12 > pb.ngan_sach;
""",
        "sai": [
            "SELECT pb.id, pb.ten, SUM(nv.luong) AS tong_luong_nam, pb.ngan_sach FROM phong_ban pb"
            " JOIN nhan_vien nv ON nv.phong_id = pb.id GROUP BY pb.id, pb.ten, pb.ngan_sach"
            " HAVING SUM(nv.luong) > pb.ngan_sach",
            "SELECT pb.id, pb.ten, SUM(nv.luong) * 12 AS tong_luong_nam, pb.ngan_sach FROM phong_ban pb"
            " JOIN nhan_vien nv ON nv.phong_id = pb.id GROUP BY pb.id, pb.ten, pb.ngan_sach",
        ],
    },
]
