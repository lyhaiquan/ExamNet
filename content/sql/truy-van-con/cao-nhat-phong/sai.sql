-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten, phong_id, luong FROM nhan_vien WHERE luong = (SELECT MAX(luong) FROM nhan_vien);

SELECT id, ho_ten, phong_id, luong FROM nhan_vien WHERE luong IN (SELECT MAX(luong) FROM nhan_vien GROUP BY phong_id);
