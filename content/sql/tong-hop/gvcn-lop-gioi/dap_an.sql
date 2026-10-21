SELECT g.id, g.ho_ten, l.ten_lop
FROM giang_vien g
JOIN lop l ON l.gvcn_id = g.id
JOIN sinh_vien s ON s.lop_id = l.id
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.lan_thi = 1 AND k.diem IS NOT NULL
GROUP BY g.id, g.ho_ten, l.id, l.ten_lop
HAVING AVG(k.diem) > (SELECT AVG(diem) FROM ket_qua WHERE lan_thi = 1);
