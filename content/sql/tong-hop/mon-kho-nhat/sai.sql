-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT m.ma_mon, m.ten_mon, ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS t FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY m.id, m.ma_mon, m.ten_mon ORDER BY t DESC LIMIT 1;

SELECT m.ma_mon, m.ten_mon, ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS t FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.lan_thi = 1 GROUP BY m.id, m.ma_mon, m.ten_mon ORDER BY t DESC LIMIT 1;

SELECT m.ma_mon, m.ten_mon, ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS t FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.diem IS NOT NULL GROUP BY m.id, m.ma_mon, m.ten_mon ORDER BY t DESC LIMIT 1;
