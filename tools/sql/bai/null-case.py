"""sql.null-case — NULL và CASE (docs/plans/danh-muc-bai.md)."""

BAI = [
    {
        "id": "chua-phan-phong",
        "ten": "Số nhân viên mỗi phòng, kể cả chưa phân phòng",
        "loai": "bai-tap",
        "csdl": "nhan-su",
        "bang": ["nhan_vien", "phong_ban"],
        "de": """
Cho biết **tên phòng và số nhân viên** của mỗi phòng có nhân viên. Nhân viên chưa xếp phòng (`phong_id` NULL) gom
thành một dòng có tên phòng là **`Chưa phân`**.

`JOIN` thường bỏ mất nhân viên không ghép được với phòng nào; dùng `LEFT JOIN` để giữ họ, rồi
`COALESCE(a, b)` trả `b` khi `a` là NULL.
""",
        "cot": ["phong", "so_nhan_vien"],
        "dap_an": """
SELECT COALESCE(p.ten, 'Chưa phân') AS phong, COUNT(*) AS so_nhan_vien
FROM nhan_vien n
LEFT JOIN phong_ban p ON p.id = n.phong_id
GROUP BY n.phong_id, p.ten;
""",
        "sai": [
            "SELECT p.ten, COUNT(*) FROM nhan_vien n JOIN phong_ban p ON p.id = n.phong_id GROUP BY n.phong_id, p.ten",
            "SELECT p.ten, COUNT(*) FROM nhan_vien n LEFT JOIN phong_ban p ON p.id = n.phong_id"
            " GROUP BY n.phong_id, p.ten",
        ],
    },
    {
        "id": "muc-ton-kho",
        "ten": "Phân loại tồn kho Hết / Ít / Nhiều",
        "loai": "bai-tap",
        "csdl": "ban-hang",
        "bang": ["san_pham"],
        "them_du_lieu": """
INSERT INTO san_pham VALUES
    (11, 'Bàn phím cơ', 1, 1200000, 10);
""",
        "de": """
Với mỗi sản phẩm, in **mã, tên** và **mức tồn kho**: `Hết` khi `ton_kho` bằng 0, `Ít` khi từ 1 đến 9, `Nhiều`
khi từ 10 trở lên.

`CASE WHEN … THEN … WHEN … ELSE … END` thử các điều kiện **theo thứ tự** và dừng ở điều kiện đúng đầu tiên.
""",
        "cot": ["id", "ten", "muc"],
        "dap_an": """
SELECT id, ten,
       CASE WHEN ton_kho = 0 THEN 'Hết'
            WHEN ton_kho < 10 THEN 'Ít'
            ELSE 'Nhiều' END AS muc
FROM san_pham;
""",
        "sai": [
            "SELECT id, ten, CASE WHEN ton_kho < 10 THEN 'Ít' WHEN ton_kho = 0 THEN 'Hết' ELSE 'Nhiều' END"
            " FROM san_pham",
            "SELECT id, ten, CASE WHEN ton_kho = 0 THEN 'Hết' WHEN ton_kho <= 10 THEN 'Ít' ELSE 'Nhiều' END"
            " FROM san_pham",
        ],
    },
    {
        "id": "dem-co-diem",
        "ten": "Đếm lần thi có điểm và vắng thi",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua"],
        "de": """
Trên bảng `ket_qua`, cho biết trong một dòng: **tổng số lần thi**, **số lần có điểm**, **số lần vắng thi**
(`diem` NULL).

`COUNT(*)` đếm mọi dòng, `COUNT(diem)` chỉ đếm dòng có `diem` khác NULL.
""",
        "cot": ["so_lan_thi", "co_diem", "vang_thi"],
        "dap_an": """
SELECT COUNT(*) AS so_lan_thi, COUNT(diem) AS co_diem, COUNT(*) - COUNT(diem) AS vang_thi
FROM ket_qua;
""",
        "sai": [
            "SELECT COUNT(*), COUNT(diem), COUNT(diem IS NULL) FROM ket_qua",
            "SELECT COUNT(*), COUNT(*), 0 FROM ket_qua",
        ],
    },
]
