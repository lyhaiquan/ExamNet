-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT mon_hoc_id, COUNT(*) FROM ket_qua WHERE diem < 4 GROUP BY mon_hoc_id ORDER BY COUNT(*) DESC LIMIT 1;

SELECT mon_hoc_id, COUNT(*) FROM ket_qua WHERE diem <= 4 GROUP BY mon_hoc_id HAVING COUNT(*) = (SELECT COUNT(*) FROM ket_qua WHERE diem <= 4 GROUP BY mon_hoc_id ORDER BY COUNT(*) DESC LIMIT 1);
