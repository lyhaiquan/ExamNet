-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten FROM giang_vien WHERE luong > 20000000 UNION SELECT g.id, g.ho_ten FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id;

SELECT id, ho_ten FROM giang_vien WHERE luong > 20000000;
