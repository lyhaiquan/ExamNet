"""sql.tong-hop — bài tổng hợp trên CSDL truong-hoc (docs/plans/danh-muc-bai.md)."""

# Dữ liệu mẫu gốc chỉ có điểm môn 1: thêm điểm môn 2 (Cơ sở dữ liệu) cho các bài cần nhiều môn.
# Sinh viên 2 trượt lần 1 rồi đạt lần 2; sinh viên 5 trượt cả hai lần; sinh viên 6 vắng thi.
MON_2 = """
INSERT INTO ket_qua VALUES
    (1, 2, 1, 9.0),
    (2, 2, 1, 3.5),
    (2, 2, 2, 6.5),
    (3, 2, 1, 8.0),
    (4, 2, 1, 7.0),
    (5, 2, 1, 2.0),
    (5, 2, 2, 3.0),
    (6, 2, 1, NULL),
    (8, 2, 1, 9.0);
"""

BAI = [
    {
        "id": "hoc-bong",
        "ten": "Sinh viên đủ điều kiện học bổng",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "ket_qua"],
        "de": """
Sinh viên được học bổng khi **điểm trung bình ≥ 8** và **không có điểm nào dưới 5**. Liệt kê **mã, họ tên** và
**điểm trung bình** (làm tròn 2 chữ số) của các sinh viên đó.

Chỉ xét **điểm lần thi 1** (`lan_thi = 1`); lần thi vắng (`diem` NULL) không tính vào trung bình. Sinh viên không
có điểm lần 1 nào thì không xét.

Gợi ý: gộp nhóm theo sinh viên, đặt cả hai điều kiện trong `HAVING` (`AVG` và `MIN`).
""",
        "them_du_lieu": MON_2 + """
INSERT INTO ket_qua VALUES
    (9, 2, 1, 10.0),
    (9, 3, 1, 4.5);
""",
        "dap_an": """
SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) AS diem_tb
FROM sinh_vien s
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY s.id, s.ho_ten
HAVING AVG(k.diem) >= 8 AND MIN(k.diem) >= 5;
""",
        "sai": [
            "SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY s.id, s.ho_ten HAVING AVG(k.diem) >= 8",
            "SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY s.id, s.ho_ten"
            " HAVING AVG(k.diem) > 8 AND MIN(k.diem) >= 5",
        ],
    },
    {
        "id": "thu-khoa-lop",
        "ten": "Thủ khoa từng lớp",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "lop", "ket_qua"],
        "de": """
Với **mỗi lớp**, tìm sinh viên có **điểm trung bình cao nhất** trong lớp. Liệt kê **tên lớp, mã và họ tên sinh
viên, điểm trung bình** (làm tròn 2 chữ số). **Bằng nhau thì in tất cả** những người cùng cao nhất.

Điểm trung bình tính trên **điểm lần thi 1**, bỏ lần vắng thi (`diem` NULL). Sinh viên chưa xếp lớp hoặc không có
điểm lần 1 nào thì không xét. Lớp không có ai được xét thì không có trong kết quả.

Gợi ý: gộp nhóm theo lớp và sinh viên, rồi trong `HAVING` so trung bình của sinh viên với trung bình cao nhất
**của chính lớp đó** (một câu con tương quan theo lớp).
""",
        "them_du_lieu": MON_2,
        "dap_an": """
SELECT l.ten_lop, s.id, s.ho_ten, ROUND(AVG(k.diem), 2) AS diem_tb
FROM sinh_vien s
JOIN lop l ON l.id = s.lop_id
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY l.id, l.ten_lop, s.id, s.ho_ten
HAVING AVG(k.diem) = (
    SELECT AVG(k2.diem)
    FROM sinh_vien s2
    JOIN ket_qua k2 ON k2.sinh_vien_id = s2.id
    WHERE s2.lop_id = l.id AND k2.lan_thi = 1 AND k2.diem IS NOT NULL
    GROUP BY s2.id
    ORDER BY AVG(k2.diem) DESC
    LIMIT 1
);
""",
        "sai": [
            "SELECT l.ten_lop, s.id, s.ho_ten, ROUND(AVG(k.diem), 2) AS diem_tb FROM sinh_vien s"
            " JOIN lop l ON l.id = s.lop_id JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY l.id, l.ten_lop, s.id, s.ho_ten"
            " ORDER BY diem_tb DESC LIMIT 1",
            "SELECT l.ten_lop, s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s"
            " JOIN lop l ON l.id = s.lop_id JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY l.id, l.ten_lop, s.id, s.ho_ten"
            " HAVING AVG(k.diem) = (SELECT AVG(k2.diem) FROM sinh_vien s2 JOIN ket_qua k2 ON k2.sinh_vien_id = s2.id"
            " WHERE s2.lop_id IS NOT NULL AND k2.lan_thi = 1 AND k2.diem IS NOT NULL GROUP BY s2.id"
            " ORDER BY AVG(k2.diem) DESC LIMIT 1)",
        ],
    },
    {
        "id": "mon-kho-nhat",
        "ten": "Môn có tỉ lệ trượt lần 1 cao nhất",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["mon_hoc", "ket_qua"],
        "de": """
Tìm môn có **tỉ lệ trượt ở lần thi 1 cao nhất**. Liệt kê **mã môn, tên môn** và **tỉ lệ trượt** tính theo phần
trăm, làm tròn 2 chữ số. **Bằng nhau thì in tất cả.**

- Chỉ xét **lần thi 1** có điểm; lần vắng thi (`diem` NULL) không tính vào cả tử lẫn mẫu.
- **Trượt** là điểm **dưới 5**.
- Tỉ lệ trượt = số lần trượt × 100 / số lần thi có điểm. Môn chưa có lần thi 1 nào có điểm thì không xét.

Nhớ nhân `100.0` (số thực) trước khi chia, nếu không SQLite chia nguyên ra 0.
""",
        "them_du_lieu": MON_2 + """
INSERT INTO ket_qua VALUES
    (1, 3, 1, 4.0),
    (3, 3, 1, 8.0),
    (4, 3, 1, 6.0);
""",
        "dap_an": """
SELECT m.ma_mon, m.ten_mon,
       ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS ty_le_truot
FROM mon_hoc m
JOIN ket_qua k ON k.mon_hoc_id = m.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY m.id, m.ma_mon, m.ten_mon
HAVING 1.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*) = (
    SELECT 1.0 * SUM(CASE WHEN k2.diem < 5 THEN 1 ELSE 0 END) / COUNT(*)
    FROM ket_qua k2
    WHERE k2.lan_thi = 1 AND k2.diem IS NOT NULL
    GROUP BY k2.mon_hoc_id
    ORDER BY 1.0 * SUM(CASE WHEN k2.diem < 5 THEN 1 ELSE 0 END) / COUNT(*) DESC
    LIMIT 1
);
""",
        "sai": [
            "SELECT m.ma_mon, m.ten_mon, ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS t"
            " FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL"
            " GROUP BY m.id, m.ma_mon, m.ten_mon ORDER BY t DESC LIMIT 1",
            "SELECT m.ma_mon, m.ten_mon, ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS t"
            " FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.lan_thi = 1"
            " GROUP BY m.id, m.ma_mon, m.ten_mon ORDER BY t DESC LIMIT 1",
            "SELECT m.ma_mon, m.ten_mon, ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS t"
            " FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.diem IS NOT NULL"
            " GROUP BY m.id, m.ma_mon, m.ten_mon ORDER BY t DESC LIMIT 1",
        ],
    },
    {
        "id": "thi-lai-dat",
        "ten": "Trượt lần 1, đạt lần 2",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "mon_hoc", "ket_qua"],
        "de": """
Liệt kê **mã, họ tên sinh viên** và **tên môn** của những lần sinh viên **trượt ở lần thi 1** (điểm dưới 5)
nhưng **đạt ở lần thi 2** (điểm từ 5 trở lên) **của cùng môn đó**. Mỗi cặp (sinh viên, môn) chỉ in một lần.

Gợi ý: nối `ket_qua` với chính nó, một bản cho lần 1, một bản cho lần 2, khớp cùng sinh viên và cùng môn. Dữ liệu ẩn
(và cả dữ liệu mẫu: lần 1 môn 2 của sinh viên 2) có lần thi bị ghi trùng hai lần: dùng `DISTINCT`.
""",
        "them_du_lieu": MON_2 + """
INSERT INTO ket_qua VALUES
    (2, 2, 1, 3.5);
""",
        "dap_an": """
SELECT DISTINCT s.id, s.ho_ten, m.ten_mon
FROM ket_qua k1
JOIN ket_qua k2 ON k2.sinh_vien_id = k1.sinh_vien_id
               AND k2.mon_hoc_id = k1.mon_hoc_id
               AND k2.lan_thi = 2
JOIN sinh_vien s ON s.id = k1.sinh_vien_id
JOIN mon_hoc m ON m.id = k1.mon_hoc_id
WHERE k1.lan_thi = 1 AND k1.diem < 5 AND k2.diem >= 5;
""",
        "sai": [
            "SELECT DISTINCT s.id, s.ho_ten, m.ten_mon FROM ket_qua k2 JOIN sinh_vien s ON s.id = k2.sinh_vien_id"
            " JOIN mon_hoc m ON m.id = k2.mon_hoc_id WHERE k2.lan_thi = 2 AND k2.diem >= 5",
            "SELECT DISTINCT s.id, s.ho_ten, m.ten_mon FROM ket_qua k1 JOIN ket_qua k2"
            " ON k2.sinh_vien_id = k1.sinh_vien_id AND k2.mon_hoc_id = k1.mon_hoc_id AND k2.lan_thi = 2"
            " JOIN sinh_vien s ON s.id = k1.sinh_vien_id JOIN mon_hoc m ON m.id = k1.mon_hoc_id"
            " WHERE k1.lan_thi = 1 AND k1.diem < 5 AND k2.diem > 5",
            "SELECT s.id, s.ho_ten, m.ten_mon FROM ket_qua k1 JOIN ket_qua k2"
            " ON k2.sinh_vien_id = k1.sinh_vien_id AND k2.mon_hoc_id = k1.mon_hoc_id AND k2.lan_thi = 2"
            " JOIN sinh_vien s ON s.id = k1.sinh_vien_id JOIN mon_hoc m ON m.id = k1.mon_hoc_id"
            " WHERE k1.lan_thi = 1 AND k1.diem < 5 AND k2.diem >= 5",
        ],
    },
    {
        "id": "tb-tin-chi",
        "ten": "Điểm trung bình theo tín chỉ",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "mon_hoc", "ket_qua"],
        "de": """
Tính **điểm trung bình có trọng số tín chỉ** của từng sinh viên. Liệt kê **mã, họ tên, điểm trung bình** (làm tròn
2 chữ số).

- Mỗi môn lấy **điểm cao nhất** trong các lần thi của sinh viên ở môn đó (bỏ lần vắng thi, `diem` NULL).
- Trung bình = Σ(điểm môn × số tín chỉ môn) / Σ(số tín chỉ các môn đó), chỉ tính các môn sinh viên có điểm.
- Sinh viên không có điểm nào thì không có trong kết quả.

Gợi ý: ghép mỗi sinh viên với những môn mình có điểm (`JOIN mon_hoc m ON EXISTS (...)`), lấy điểm cao nhất của
môn bằng một câu con `(SELECT MAX(diem) ...)` rồi cộng dồn theo sinh viên. Đừng lấy trung bình mọi lần thi: lần
thi lại và lần ghi trùng sẽ bị tính nhiều lần.
""",
        "them_du_lieu": MON_2 + """
INSERT INTO ket_qua VALUES
    (1, 4, 1, 6.0);
""",
        "dap_an": """
SELECT s.id, s.ho_ten,
       ROUND(SUM(m.so_tin_chi * (SELECT MAX(k.diem) FROM ket_qua k
                                 WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id))
             / SUM(m.so_tin_chi), 2) AS diem_tb
FROM sinh_vien s
JOIN mon_hoc m ON EXISTS (SELECT 1 FROM ket_qua k
                          WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id AND k.diem IS NOT NULL)
GROUP BY s.id, s.ho_ten;
""",
        "sai": [
            "SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.diem IS NOT NULL GROUP BY s.id, s.ho_ten",
            "SELECT s.id, s.ho_ten, ROUND(1.0 * SUM(k.diem * m.so_tin_chi) / SUM(m.so_tin_chi), 2) FROM sinh_vien s"
            " JOIN ket_qua k ON k.sinh_vien_id = s.id JOIN mon_hoc m ON m.id = k.mon_hoc_id"
            " WHERE k.diem IS NOT NULL GROUP BY s.id, s.ho_ten",
            "SELECT s.id, s.ho_ten, ROUND(1.0 * SUM(k.diem * m.so_tin_chi) / SUM(m.so_tin_chi), 2) FROM sinh_vien s"
            " JOIN ket_qua k ON k.sinh_vien_id = s.id JOIN mon_hoc m ON m.id = k.mon_hoc_id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY s.id, s.ho_ten",
        ],
    },
    {
        "id": "lop-deu-thi",
        "ten": "Lớp mà mọi sinh viên đều đã thi",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["lop", "sinh_vien", "ket_qua"],
        "de": """
Liệt kê **mã và tên lớp** của các lớp mà **mọi sinh viên trong lớp** đều **đã thi ít nhất một môn**.

- "Đã thi" nghĩa là có ít nhất một dòng `ket_qua` **có điểm**; chỉ có lần vắng thi (`diem` NULL) thì chưa tính
  là đã thi.
- Lớp **chưa có sinh viên nào** thì **không** đưa vào kết quả.

Gợi ý: "mọi sinh viên đều đã thi" ⇔ "không có sinh viên nào chưa thi": `NOT EXISTS` lồng `NOT EXISTS`.
""",
        "them_du_lieu": """
INSERT INTO sinh_vien VALUES
    (11, 'Lý Thị Mơ', 'Nữ', '2005-06-06', 'Huế', 3),
    (12, 'Tạ Văn Phong', 'Nam', '2004-10-10', 'Hà Nội', 2);

INSERT INTO ket_qua VALUES
    (12, 1, 1, NULL);
""",
        "dap_an": """
SELECT l.id, l.ten_lop
FROM lop l
WHERE EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id)
  AND NOT EXISTS (
      SELECT 1
      FROM sinh_vien s
      WHERE s.lop_id = l.id
        AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.diem IS NOT NULL)
  );
""",
        "sai": [
            "SELECT l.id, l.ten_lop FROM lop l WHERE NOT EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id"
            " AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.diem IS NOT NULL))",
            "SELECT l.id, l.ten_lop FROM lop l WHERE EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id)"
            " AND NOT EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id"
            " AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id))",
            "SELECT DISTINCT l.id, l.ten_lop FROM lop l JOIN sinh_vien s ON s.lop_id = l.id"
            " JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.diem IS NOT NULL",
        ],
    },
    {
        "id": "que-dong-nhat",
        "ten": "Quê đông sinh viên nhất trong từng khoa",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "lop"],
        "de": """
Khoa của sinh viên là khoa của **lớp** sinh viên đó (`lop.khoa`). Với **mỗi khoa**, tìm **quê quán có nhiều sinh
viên nhất**. Liệt kê **khoa, quê quán, số sinh viên**. **Bằng nhau thì in tất cả.**

Sinh viên chưa xếp lớp hoặc chưa ghi quê (`que_quan` NULL) không tính. Khoa không có sinh viên nào thì không có
trong kết quả.

Gợi ý: gộp nhóm theo (khoa, quê), rồi trong `HAVING` so số sinh viên với số lớn nhất **của chính khoa đó**.
""",
        "dap_an": """
SELECT l.khoa, s.que_quan, COUNT(*) AS so_sv
FROM sinh_vien s
JOIN lop l ON l.id = s.lop_id
WHERE s.que_quan IS NOT NULL
GROUP BY l.khoa, s.que_quan
HAVING COUNT(*) = (
    SELECT COUNT(*)
    FROM sinh_vien s2
    JOIN lop l2 ON l2.id = s2.lop_id
    WHERE l2.khoa = l.khoa AND s2.que_quan IS NOT NULL
    GROUP BY s2.que_quan
    ORDER BY COUNT(*) DESC
    LIMIT 1
);
""",
        "sai": [
            "SELECT l.khoa, s.que_quan, COUNT(*) AS so_sv FROM sinh_vien s JOIN lop l ON l.id = s.lop_id"
            " WHERE s.que_quan IS NOT NULL GROUP BY l.khoa, s.que_quan ORDER BY so_sv DESC LIMIT 1",
            "SELECT l.khoa, s.que_quan, COUNT(*) FROM sinh_vien s JOIN lop l ON l.id = s.lop_id"
            " WHERE s.que_quan IS NOT NULL GROUP BY l.khoa, s.que_quan HAVING COUNT(*) = (SELECT COUNT(*)"
            " FROM sinh_vien s2 JOIN lop l2 ON l2.id = s2.lop_id WHERE s2.que_quan IS NOT NULL"
            " GROUP BY l2.khoa, s2.que_quan ORDER BY COUNT(*) DESC LIMIT 1)",
        ],
    },
    {
        "id": "gvcn-lop-gioi",
        "ten": "Chủ nhiệm lớp học giỏi hơn toàn trường",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["giang_vien", "lop", "sinh_vien", "ket_qua"],
        "de": """
Liệt kê **mã, họ tên giảng viên** và **tên lớp** mà giảng viên đó chủ nhiệm, với điều kiện **điểm trung bình của
lớp cao hơn điểm trung bình toàn trường**.

- Chỉ xét **điểm lần thi 1**, bỏ lần vắng thi (`diem` NULL).
- Trung bình của lớp: trung bình mọi điểm lần 1 của các sinh viên trong lớp.
- Trung bình toàn trường: trung bình **mọi** điểm lần 1 trong `ket_qua`, **kể cả** sinh viên chưa xếp lớp.
- Lớp không có điểm nào hoặc chưa có chủ nhiệm thì không xét. Giảng viên chủ nhiệm nhiều lớp đạt thì in mỗi lớp
  một dòng.

Đừng chép số trung bình nhìn thấy vào câu: dữ liệu ẩn khác.
""",
        "them_du_lieu": """
INSERT INTO sinh_vien VALUES
    (11, 'Lý Thị Mơ', 'Nữ', '2005-06-06', 'Huế', 1);

INSERT INTO ket_qua VALUES
    (11, 1, 1, 8.0);
""",
        "dap_an": """
SELECT g.id, g.ho_ten, l.ten_lop
FROM giang_vien g
JOIN lop l ON l.gvcn_id = g.id
JOIN sinh_vien s ON s.lop_id = l.id
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY g.id, g.ho_ten, l.id, l.ten_lop
HAVING AVG(k.diem) > (SELECT AVG(diem) FROM ket_qua WHERE lan_thi = 1);
""",
        "sai": [
            "SELECT g.id, g.ho_ten, l.ten_lop FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id"
            " JOIN sinh_vien s ON s.lop_id = l.id JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY g.id, g.ho_ten, l.id, l.ten_lop"
            " HAVING AVG(k.diem) > (SELECT AVG(k2.diem) FROM ket_qua k2 JOIN sinh_vien s2 ON s2.id = k2.sinh_vien_id"
            " WHERE k2.lan_thi = 1 AND s2.lop_id IS NOT NULL)",
            "SELECT g.id, g.ho_ten, l.ten_lop FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id"
            " JOIN sinh_vien s ON s.lop_id = l.id JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY g.id, g.ho_ten, l.id, l.ten_lop"
            " HAVING AVG(k.diem) > 7.55",
        ],
    },
    {
        "id": "chua-thi-bat-buoc",
        "ten": "Sinh viên chưa thi môn bắt buộc nào",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["sinh_vien", "ket_qua"],
        "de": """
Hai môn **bắt buộc** là môn có mã số (`mon_hoc.id`) **1** và **2**. Liệt kê **mã và họ tên** các sinh viên **chưa
thi môn nào** trong hai môn này.

"Đã thi" một môn nghĩa là có dòng `ket_qua` của môn đó **có điểm**; chỉ có lần vắng thi (`diem` NULL) thì vẫn là
chưa thi. Sinh viên chỉ thi các môn khác cũng là chưa thi môn bắt buộc nào.
""",
        "them_du_lieu": """
INSERT INTO sinh_vien VALUES
    (11, 'Lý Thị Mơ', 'Nữ', '2005-06-06', 'Huế', 3),
    (12, 'Tạ Văn Phong', 'Nam', '2004-10-10', NULL, 2);

INSERT INTO ket_qua VALUES
    (12, 3, 1, 8.0);
""",
        "dap_an": """
SELECT s.id, s.ho_ten
FROM sinh_vien s
WHERE NOT EXISTS (
    SELECT 1
    FROM ket_qua k
    WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id IN (1, 2) AND k.diem IS NOT NULL
);
""",
        "sai": [
            "SELECT id, ho_ten FROM sinh_vien WHERE id NOT IN (SELECT sinh_vien_id FROM ket_qua)",
            "SELECT s.id, s.ho_ten FROM sinh_vien s WHERE NOT EXISTS (SELECT 1 FROM ket_qua k"
            " WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id IN (1, 2))",
            "SELECT s.id, s.ho_ten FROM sinh_vien s LEFT JOIN ket_qua k ON k.sinh_vien_id = s.id"
            " WHERE k.sinh_vien_id IS NULL",
        ],
    },
    {
        "id": "pho-diem",
        "ten": "Phổ điểm theo khoảng",
        "loai": "luyen-tap",
        "csdl": "truong-hoc",
        "bang": ["ket_qua"],
        "de": """
Đếm **số lần thi** rơi vào từng khoảng điểm: `[0, 4)`, `[4, 6.5)`, `[6.5, 8)`, `[8, 10]` (ngoặc vuông: lấy đầu mút,
ngoặc tròn: không lấy). Mỗi dòng kết quả gồm **nhãn khoảng** (viết đúng như trên, ví dụ `[4, 6.5)`) và **số lần
thi**.

Tính mọi lần thi (cả lần 1 và lần 2), bỏ lần vắng thi (`diem` NULL). Chỉ liệt kê khoảng có ít nhất một lần thi.

Gợi ý: `CASE WHEN ... THEN '[0, 4)' ... END` cho nhãn, rồi `GROUP BY` theo nhãn đó. Cẩn thận điểm đúng bằng 6.5
và 8.
""",
        "them_du_lieu": MON_2,
        "dap_an": """
SELECT CASE
           WHEN diem < 4 THEN '[0, 4)'
           WHEN diem < 6.5 THEN '[4, 6.5)'
           WHEN diem < 8 THEN '[6.5, 8)'
           ELSE '[8, 10]'
       END AS khoang,
       COUNT(*) AS so_lan
FROM ket_qua
WHERE diem IS NOT NULL
GROUP BY khoang;
""",
        "sai": [
            "SELECT CASE WHEN diem <= 4 THEN '[0, 4)' WHEN diem <= 6.5 THEN '[4, 6.5)' WHEN diem <= 8 THEN '[6.5, 8)'"
            " ELSE '[8, 10]' END AS khoang, COUNT(*) FROM ket_qua WHERE diem IS NOT NULL GROUP BY khoang",
            "SELECT CASE WHEN diem < 4 THEN '[0, 4)' WHEN diem < 6.5 THEN '[4, 6.5)' WHEN diem < 8 THEN '[6.5, 8)'"
            " ELSE '[8, 10]' END AS khoang, COUNT(*) FROM ket_qua GROUP BY khoang",
        ],
    },
]
