SELECT l.ten_lop, AVG(k.diem) AS diem_tb
FROM ket_qua k
JOIN sinh_vien s ON k.sinh_vien_id = s.id
JOIN lop l ON s.lop_id = l.id
WHERE k.lan_thi = 1
GROUP BY l.ten_lop
HAVING AVG(k.diem) > 7;
