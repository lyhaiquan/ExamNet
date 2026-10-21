-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT p.ten, COUNT(*) FROM nhan_vien n JOIN phong_ban p ON p.id = n.phong_id GROUP BY n.phong_id, p.ten;

SELECT p.ten, COUNT(*) FROM nhan_vien n LEFT JOIN phong_ban p ON p.id = n.phong_id GROUP BY n.phong_id, p.ten;
