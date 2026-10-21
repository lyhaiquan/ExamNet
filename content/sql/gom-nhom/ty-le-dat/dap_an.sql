SELECT mon_hoc_id,
       ROUND(100.0 * SUM(CASE WHEN diem >= 4 THEN 1 ELSE 0 END) / COUNT(diem), 2) AS ty_le
FROM ket_qua
WHERE diem IS NOT NULL
GROUP BY mon_hoc_id;
