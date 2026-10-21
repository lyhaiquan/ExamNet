-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT lop_id, COUNT(*) FROM sinh_vien GROUP BY lop_id HAVING COUNT(*) >= 3;

SELECT lop_id, COUNT(*) FROM sinh_vien WHERE lop_id IS NOT NULL GROUP BY lop_id HAVING COUNT(*) > 3;
