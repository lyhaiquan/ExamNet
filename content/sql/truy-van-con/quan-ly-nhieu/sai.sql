-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten FROM nhan_vien WHERE id IN (SELECT quan_ly_id FROM nhan_vien GROUP BY quan_ly_id HAVING COUNT(*) > 3);

SELECT id, ho_ten FROM nhan_vien WHERE quan_ly_id IN (SELECT quan_ly_id FROM nhan_vien GROUP BY quan_ly_id HAVING COUNT(*) >= 3);
