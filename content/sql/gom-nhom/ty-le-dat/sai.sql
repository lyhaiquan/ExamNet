-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT mon_hoc_id, ROUND(100.0 * SUM(CASE WHEN diem >= 4 THEN 1 ELSE 0 END) / COUNT(*), 2) FROM ket_qua GROUP BY mon_hoc_id;

SELECT mon_hoc_id, ROUND(100 * SUM(CASE WHEN diem >= 4 THEN 1 ELSE 0 END) / COUNT(diem), 2) FROM ket_qua WHERE diem IS NOT NULL GROUP BY mon_hoc_id;
