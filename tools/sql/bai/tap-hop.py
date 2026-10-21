"""sql.tap-hop — UNION, UNION ALL, INTERSECT, EXCEPT (docs/plans/danh-muc-bai.md)."""

# Kết quả môn 2 (Cơ sở dữ liệu) thêm vào dữ liệu mẫu: dữ liệu mẫu chung chỉ có điểm môn 1
KET_QUA_MON_2 = """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 7.5),
    (3, 2, 1, 5.0),
    (5, 2, 1, 8.0),
    (6, 2, 1, NULL),
    (8, 2, 1, 6.5);
"""

BAI = [
    {
        "id": "moi-nguoi-trong-truong",
        "ten": "Họ tên mọi người trong trường",
        "loai": "bai-tap",
        "bac": ["du-doan", "code"],
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "giang_vien"],
        "de": """
Liệt kê **họ tên** của mọi người trong trường: cả **sinh viên** lẫn **giảng viên**. Mỗi họ tên chỉ xuất hiện
**một lần**, kể cả khi nhiều người trùng tên.

`A UNION B` ghép kết quả hai câu (cùng số cột) rồi **bỏ dòng trùng**; `UNION ALL` ghép mà giữ nguyên mọi dòng.
""",
        "dap_an": """
SELECT ho_ten FROM sinh_vien
UNION
SELECT ho_ten FROM giang_vien;
""",
        "sai": [
            "SELECT ho_ten FROM sinh_vien UNION ALL SELECT ho_ten FROM giang_vien",
            "SELECT ho_ten FROM sinh_vien",
        ],
    },
    {
        "id": "thi-a-chua-b",
        "ten": "Đã thi Lập trình mạng nhưng chưa thi Cơ sở dữ liệu",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua", "mon_hoc"],
        "them_du_lieu": KET_QUA_MON_2,
        "de": """
Liệt kê **mã sinh viên** của những người **đã thi** môn **Lập trình mạng** (`mon_hoc_id = 1`) nhưng **chưa thi**
môn **Cơ sở dữ liệu** (`mon_hoc_id = 2`). "Đã thi" một môn là có ít nhất một lần thi môn đó **có điểm** (vắng
thi, `diem` NULL, không tính).

`A EXCEPT B` giữ các dòng của A **không có** trong B (và bỏ trùng). Đổi chỗ A và B là ra câu trả lời khác hẳn.
""",
        "dap_an": """
SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 1 AND diem IS NOT NULL
EXCEPT
SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 2 AND diem IS NOT NULL;
""",
        "sai": [
            "SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 1"
            " EXCEPT SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 2",
            "SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 2 AND diem IS NOT NULL"
            " EXCEPT SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 1 AND diem IS NOT NULL",
        ],
    },
    {
        "id": "que-co-trung",
        "ten": "Quê quán sinh viên hai lớp, giữ trùng",
        "loai": "bai-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien"],
        "de": """
Nhà trường cần thống kê quê quán của sinh viên **lớp 1** và **lớp 2** (`lop_id` bằng 1 hoặc 2). Ghép **quê
quán** của sinh viên lớp 1 với quê quán của sinh viên lớp 2 thành một danh sách **giữ nguyên số lần xuất hiện**:
quê có 4 sinh viên thì có 4 dòng.

So sánh: `UNION` bỏ dòng trùng, `UNION ALL` thì không. Bài này cần cái nào?
""",
        "dap_an": """
SELECT que_quan FROM sinh_vien WHERE lop_id = 1
UNION ALL
SELECT que_quan FROM sinh_vien WHERE lop_id = 2;
""",
        "sai": [
            "SELECT que_quan FROM sinh_vien WHERE lop_id = 1 UNION SELECT que_quan FROM sinh_vien WHERE lop_id = 2",
        ],
    },
    {
        "id": "ban-ca-hai-thang",
        "ten": "Sản phẩm bán được cả tháng 8 và tháng 9/2024",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["don_hang", "chi_tiet_don"],
        "de": """
Liệt kê **mã sản phẩm** bán được trong **cả tháng 8/2024 lẫn tháng 9/2024**. Sản phẩm "bán được" trong một tháng
nếu nó có trong chi tiết của một đơn **đặt trong tháng đó** và đơn **không bị huỷ** (`trang_thai <> 'huy'`).

Viết hai câu (một câu cho mỗi tháng, đều `JOIN` `chi_tiet_don` với `don_hang`) rồi lấy phần chung bằng
`INTERSECT`.
""",
        "dap_an": """
SELECT c.san_pham_id
FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id
WHERE d.ngay_dat >= '2024-08-01' AND d.ngay_dat < '2024-09-01' AND d.trang_thai <> 'huy'
INTERSECT
SELECT c.san_pham_id
FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id
WHERE d.ngay_dat >= '2024-09-01' AND d.ngay_dat < '2024-10-01' AND d.trang_thai <> 'huy';
""",
        "sai": [
            "SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE d.ngay_dat >= '2024-08-01' AND d.ngay_dat < '2024-09-01'"
            " INTERSECT SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE d.ngay_dat >= '2024-09-01' AND d.ngay_dat < '2024-10-01'",
            "SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE d.ngay_dat >= '2024-08-01' AND d.ngay_dat < '2024-09-01' AND d.trang_thai <> 'huy'"
            " UNION SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id"
            " WHERE d.ngay_dat >= '2024-09-01' AND d.ngay_dat < '2024-10-01' AND d.trang_thai <> 'huy'",
        ],
    },
    {
        "id": "nv-ngoai-du-an",
        "ten": "Nhân viên không tham gia dự án nào",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien", "phan_cong"],
        "de": """
Liệt kê **mã và họ tên** các nhân viên **không được phân công** vào dự án nào.

Lấy toàn bộ nhân viên `EXCEPT` những nhân viên có dòng trong `phan_cong`. Hai vế của `EXCEPT` phải cùng số cột:
vế sau cũng lấy `id, ho_ten` (nối `nhan_vien` với `phan_cong`).
""",
        "dap_an": """
SELECT id, ho_ten FROM nhan_vien
EXCEPT
SELECT n.id, n.ho_ten FROM nhan_vien n JOIN phan_cong p ON p.nhan_vien_id = n.id;
""",
        "sai": [
            "SELECT id, ho_ten FROM nhan_vien EXCEPT SELECT n.id, n.ho_ten FROM nhan_vien n"
            " JOIN phan_cong p ON p.du_an_id = n.id",
            "SELECT n.id, n.ho_ten FROM nhan_vien n JOIN phan_cong p ON p.nhan_vien_id = n.id",
        ],
    },
    {
        "id": "ton-va-het",
        "ten": "Tồn mà chưa bán, cùng đã bán mà hết hàng",
        "loai": "luyen-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham", "chi_tiet_don"],
        "them_du_lieu": """
INSERT INTO san_pham VALUES
    (11, 'Tai nghe Sony', 1, 3000000, 15),
    (12, 'Bàn phím cơ', 1, 1500000, 0);
""",
        "de": """
Liệt kê **mã và tên** các sản phẩm thuộc một trong hai nhóm cần xử lý:

- **còn hàng** (`ton_kho > 0`) mà **chưa từng bán** (không có trong `chi_tiet_don`);
- **đã bán** ít nhất một lần mà nay **hết hàng** (`ton_kho = 0`).

Mỗi nhóm là một câu `SELECT` riêng, ghép lại bằng `UNION`. "Đã bán" hay "chưa từng bán" kiểm bằng câu con trên
`chi_tiet_don` (`IN` / `NOT IN`, hoặc `EXISTS` / `NOT EXISTS`).
""",
        "dap_an": """
SELECT id, ten FROM san_pham
WHERE ton_kho > 0 AND id NOT IN (SELECT san_pham_id FROM chi_tiet_don)
UNION
SELECT id, ten FROM san_pham
WHERE ton_kho = 0 AND id IN (SELECT san_pham_id FROM chi_tiet_don);
""",
        "sai": [
            "SELECT id, ten FROM san_pham WHERE ton_kho > 0 AND id NOT IN (SELECT san_pham_id FROM chi_tiet_don)"
            " UNION SELECT id, ten FROM san_pham WHERE ton_kho = 0",
            "SELECT id, ten FROM san_pham WHERE ton_kho > 0 EXCEPT SELECT s.id, s.ten FROM san_pham s"
            " JOIN chi_tiet_don c ON c.san_pham_id = s.id",
        ],
    },
    {
        "id": "gv-chu-nhiem-luong",
        "ten": "Giảng viên vừa chủ nhiệm vừa lương trên 20 triệu",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["giang_vien", "lop"],
        "de": """
Liệt kê **mã và họ tên** các giảng viên **vừa** đang chủ nhiệm ít nhất một lớp **vừa** có lương **trên
20 000 000**. Dùng `INTERSECT`.
""",
        "dap_an": """
SELECT id, ho_ten FROM giang_vien WHERE luong > 20000000
INTERSECT
SELECT g.id, g.ho_ten FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id;
""",
        "sai": [
            "SELECT id, ho_ten FROM giang_vien WHERE luong > 20000000"
            " UNION SELECT g.id, g.ho_ten FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id",
            "SELECT id, ho_ten FROM giang_vien WHERE luong > 20000000",
        ],
    },
]
