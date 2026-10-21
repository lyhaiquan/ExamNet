SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) AS diem_tb
FROM sinh_vien s
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY s.id, s.ho_ten
HAVING AVG(k.diem) >= 8 AND MIN(k.diem) >= 5;
