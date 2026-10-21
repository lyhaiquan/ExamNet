-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT phong_id, AVG(luong) FROM nhan_vien WHERE luong > 20000000 AND phong_id IS NOT NULL GROUP BY phong_id;

SELECT phong_id, AVG(luong) FROM nhan_vien GROUP BY phong_id HAVING AVG(luong) > 20000000;
