SELECT m.ma_mon, m.ten_mon,
       ROUND(100.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*), 2) AS ty_le_truot
FROM mon_hoc m
JOIN ket_qua k ON k.mon_hoc_id = m.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY m.id, m.ma_mon, m.ten_mon
HAVING 1.0 * SUM(CASE WHEN k.diem < 5 THEN 1 ELSE 0 END) / COUNT(*) = (
    SELECT 1.0 * SUM(CASE WHEN k2.diem < 5 THEN 1 ELSE 0 END) / COUNT(*)
    FROM ket_qua k2
    WHERE k2.lan_thi = 1 AND k2.diem IS NOT NULL
    GROUP BY k2.mon_hoc_id
    ORDER BY 1.0 * SUM(CASE WHEN k2.diem < 5 THEN 1 ELSE 0 END) / COUNT(*) DESC
    LIMIT 1
);
