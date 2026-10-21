"""sql.co-ban — SELECT, WHERE, ORDER BY, LIMIT, DISTINCT (docs/plans/danh-muc-bai.md)."""

BAI = [
    {
        "id": "sv-nu-lop",
        "ten": "Sinh viên nữ của lớp số 2",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "truong-hoc",
        "bang": ["sinh_vien"],
        "de": """
Liệt kê **mã và họ tên** các sinh viên **nữ** thuộc lớp có mã lớp (`lop_id`) bằng **2**.

Hai điều kiện phải cùng đúng nên nối bằng `AND`. Giới tính lưu đúng chữ `'Nữ'` (có dấu).
""",
        "dap_an": """
SELECT id, ho_ten
FROM sinh_vien
WHERE gioi_tinh = 'Nữ' AND lop_id = 2;
""",
        "sai": [
            "SELECT id, ho_ten FROM sinh_vien WHERE lop_id = 2",
            "SELECT id, ho_ten FROM sinh_vien WHERE gioi_tinh = 'Nữ' OR lop_id = 2",
        ],
    },
    {
        "id": "khoang-gia",
        "ten": "Sản phẩm giá từ 100 000 đến 500 000 còn hàng",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham"],
        "de": """
Liệt kê **mã, tên, giá** các sản phẩm có giá **từ 100 000 đến 500 000** (tính cả hai đầu) và **còn hàng**
(`ton_kho` lớn hơn 0).

`gia BETWEEN a AND b` nghĩa là `gia >= a AND gia <= b`: **lấy cả hai đầu mút**. Dữ liệu có sản phẩm giá đúng
100 000 và đúng 500 000.
""",
        "them_du_lieu": """
INSERT INTO san_pham VALUES
    (11, 'Chuột không dây', 1, 500000, 12),
    (12, 'Lót chuột', 1, 100000, 9),
    (13, 'Bút trình chiếu', 1, 450000, 0);
""",
        "dap_an": """
SELECT id, ten, gia
FROM san_pham
WHERE gia BETWEEN 100000 AND 500000 AND ton_kho > 0;
""",
        "sai": [
            "SELECT id, ten, gia FROM san_pham WHERE gia > 100000 AND gia < 500000 AND ton_kho > 0",
            "SELECT id, ten, gia FROM san_pham WHERE gia BETWEEN 100000 AND 500000",
        ],
    },
    {
        "id": "ho-nguyen",
        "ten": "Khách hàng họ Nguyễn",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang"],
        "de": """
Liệt kê **mã và họ tên** các khách hàng **họ Nguyễn**. Họ là chữ đầu tiên của `ho_ten`.

`LIKE` so khớp mẫu: `%` thay cho một chuỗi bất kỳ (kể cả rỗng), `_` thay cho đúng một ký tự. Cẩn thận: khách tên
"Trần Nguyễn Hà" có chữ Nguyễn nhưng **không phải** họ Nguyễn.
""",
        "them_du_lieu": """
INSERT INTO khach_hang VALUES
    (9, 'Trần Nguyễn Hà', 'Huế', '2024-08-05'),
    (10, 'Nguyễn Thị Hồng', 'Hải Phòng', '2024-08-12');
""",
        "dap_an": """
SELECT id, ho_ten
FROM khach_hang
WHERE ho_ten LIKE 'Nguyễn %';
""",
        "sai": [
            "SELECT id, ho_ten FROM khach_hang WHERE ho_ten LIKE '%Nguyễn%'",
            "SELECT id, ho_ten FROM khach_hang WHERE ho_ten = 'Nguyễn'",
        ],
    },
    {
        "id": "ba-sv-tre-nhat",
        "ten": "3 sinh viên trẻ nhất",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "thu_tu": True,
        "bang": ["sinh_vien"],
        "de": """
Liệt kê **mã, họ tên, ngày sinh** của **3 sinh viên trẻ nhất**, người trẻ nhất đứng đầu. Cùng ngày sinh thì xếp
theo **họ tên** tăng dần, cùng họ tên thì theo **mã** tăng dần.

Trẻ nhất là sinh **muộn nhất**: ngày sinh lớn nhất. Ngày lưu dạng chuỗi `YYYY-MM-DD` nên so chuỗi cũng là so ngày.
""",
        "dap_an": """
SELECT id, ho_ten, ngay_sinh
FROM sinh_vien
ORDER BY ngay_sinh DESC, ho_ten, id
LIMIT 3;
""",
        "sai": [
            "SELECT id, ho_ten, ngay_sinh FROM sinh_vien ORDER BY ngay_sinh LIMIT 3",
            "SELECT id, ho_ten, ngay_sinh FROM sinh_vien ORDER BY ngay_sinh DESC",
        ],
    },
    {
        "id": "mon-nhieu-tin-chi",
        "ten": "Môn từ 3 tín chỉ",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "thu_tu": True,
        "bang": ["mon_hoc"],
        "de": """
Liệt kê **mã môn, tên môn, số tín chỉ** của các môn có **từ 3 tín chỉ trở lên**, sắp theo số tín chỉ **giảm dần**;
cùng số tín chỉ thì theo **mã môn** tăng dần.
""",
        "them_du_lieu": """
INSERT INTO mon_hoc VALUES
    (5, 'INT1340', 'Nhập môn công nghệ phần mềm', 4);
""",
        "dap_an": """
SELECT ma_mon, ten_mon, so_tin_chi
FROM mon_hoc
WHERE so_tin_chi >= 3
ORDER BY so_tin_chi DESC, ma_mon;
""",
        "sai": [
            "SELECT ma_mon, ten_mon, so_tin_chi FROM mon_hoc WHERE so_tin_chi > 3 ORDER BY so_tin_chi DESC, ma_mon",
            "SELECT ma_mon, ten_mon, so_tin_chi FROM mon_hoc WHERE so_tin_chi >= 3 ORDER BY so_tin_chi DESC",
        ],
    },
    {
        "id": "don-thang-9",
        "ten": "Đơn hàng đặt trong tháng 9/2024",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang"],
        "de": """
Liệt kê **mã đơn, mã khách, ngày đặt** của các đơn đặt trong **tháng 9 năm 2024** (từ 01/09 đến hết 30/09).

Ngày lưu dạng chuỗi `YYYY-MM-DD`. Cách gọn: `ngay_dat >= '2024-09-01' AND ngay_dat < '2024-10-01'`, hoặc dùng hàm
ngày của SQLite `strftime('%Y-%m', ngay_dat) = '2024-09'`. Đừng bỏ sót đơn đặt đúng ngày 01/09.
""",
        "dap_an": """
SELECT id, khach_hang_id, ngay_dat
FROM don_hang
WHERE ngay_dat >= '2024-09-01' AND ngay_dat < '2024-10-01';
""",
        "sai": [
            "SELECT id, khach_hang_id, ngay_dat FROM don_hang WHERE ngay_dat > '2024-09-01' AND ngay_dat < '2024-10-01'",
            "SELECT id, khach_hang_id, ngay_dat FROM don_hang WHERE ngay_dat < '2024-10-01'",
        ],
    },
    {
        "id": "dem-thanh-pho",
        "ten": "Số thành phố khác nhau có khách hàng",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["khach_hang"],
        "de": """
Cho biết có **bao nhiêu thành phố khác nhau** có khách hàng. Khách chưa ghi thành phố (NULL) không tính.

`COUNT(*)` đếm mọi dòng, `COUNT(cot)` bỏ dòng có `cot` NULL, còn `COUNT(DISTINCT cot)` đếm số **giá trị khác
nhau** (cũng bỏ NULL).

Kết quả là một dòng, một cột.
""",
        "cot": ["so_thanh_pho"],
        "dap_an": """
SELECT COUNT(DISTINCT thanh_pho) AS so_thanh_pho
FROM khach_hang;
""",
        "sai": [
            "SELECT COUNT(thanh_pho) FROM khach_hang",
            "SELECT COUNT(*) FROM khach_hang",
        ],
    },
    {
        "id": "trang-thai-don",
        "ten": "Đơn hàng mới hoặc đang giao",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang"],
        "de": """
Liệt kê **mã đơn và trạng thái** của các đơn có trạng thái là `moi` hoặc `dang-giao`.

Dùng `IN (...)` cho gọn thay vì nhiều `OR`.
""",
        "dap_an": """
SELECT id, trang_thai
FROM don_hang
WHERE trang_thai IN ('moi', 'dang-giao');
""",
        "sai": [
            "SELECT id, trang_thai FROM don_hang WHERE trang_thai = 'moi'",
            "SELECT id, trang_thai FROM don_hang WHERE trang_thai <> 'huy'",
        ],
    },
    {
        "id": "vao-truoc-2020",
        "ten": "Nhân viên vào làm trước 2020, lương dưới 20 triệu",
        "loai": "luyen-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien"],
        "de": """
Liệt kê **mã, họ tên, lương, ngày vào làm** của các nhân viên vào làm **trước năm 2020** (trước ngày 01/01/2020)
và có lương **dưới 20 000 000**.
""",
        "dap_an": """
SELECT id, ho_ten, luong, ngay_vao_lam
FROM nhan_vien
WHERE ngay_vao_lam < '2020-01-01' AND luong < 20000000;
""",
        "sai": [
            "SELECT id, ho_ten, luong, ngay_vao_lam FROM nhan_vien WHERE ngay_vao_lam <= '2020-12-31' AND luong < 20000000",
            "SELECT id, ho_ten, luong, ngay_vao_lam FROM nhan_vien WHERE ngay_vao_lam < '2020-01-01' AND luong <= 20000000",
        ],
    },
    {
        "id": "trang-hai",
        "ten": "Trang 2 của danh sách sản phẩm",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "thu_tu": True,
        "bang": ["san_pham"],
        "de": """
Danh sách sản phẩm sắp theo **tên** tăng dần (cùng tên thì theo **mã** tăng dần) được chia trang, **mỗi trang 3
sản phẩm**. Liệt kê **mã và tên** các sản phẩm ở **trang 2** (dòng thứ 4 đến thứ 6).

`LIMIT n OFFSET k` bỏ qua `k` dòng đầu rồi lấy `n` dòng.
""",
        "dap_an": """
SELECT id, ten
FROM san_pham
ORDER BY ten, id
LIMIT 3 OFFSET 3;
""",
        "sai": [
            "SELECT id, ten FROM san_pham ORDER BY ten, id LIMIT 3 OFFSET 4",
            "SELECT id, ten FROM san_pham ORDER BY id LIMIT 3 OFFSET 3",
        ],
    },
]
