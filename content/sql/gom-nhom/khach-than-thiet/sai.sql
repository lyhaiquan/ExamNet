-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT khach_hang_id, COUNT(*) FROM don_hang GROUP BY khach_hang_id HAVING COUNT(*) >= 3;

SELECT khach_hang_id, COUNT(*) FROM don_hang WHERE khach_hang_id IS NOT NULL GROUP BY khach_hang_id HAVING COUNT(*) > 3;
