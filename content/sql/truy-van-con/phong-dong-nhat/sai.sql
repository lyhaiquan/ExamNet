-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT p.id, p.ten, COUNT(*) FROM phong_ban p JOIN nhan_vien n ON n.phong_id = p.id GROUP BY p.id, p.ten ORDER BY COUNT(*) DESC LIMIT 1;

SELECT p.id, p.ten, COUNT(*) FROM phong_ban p JOIN nhan_vien n ON n.phong_id = p.id GROUP BY p.id, p.ten HAVING COUNT(*) = (SELECT COUNT(*) FROM nhan_vien GROUP BY phong_id ORDER BY 1 LIMIT 1);
