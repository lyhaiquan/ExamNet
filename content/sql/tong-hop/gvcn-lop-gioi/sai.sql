-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT g.id, g.ho_ten, l.ten_lop FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id JOIN sinh_vien s ON s.lop_id = l.id JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY g.id, g.ho_ten, l.id, l.ten_lop HAVING AVG(k.diem) > (SELECT AVG(k2.diem) FROM ket_qua k2 JOIN sinh_vien s2 ON s2.id = k2.sinh_vien_id WHERE k2.lan_thi = 1 AND s2.lop_id IS NOT NULL);

SELECT g.id, g.ho_ten, l.ten_lop FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id JOIN sinh_vien s ON s.lop_id = l.id JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY g.id, g.ho_ten, l.id, l.ten_lop HAVING AVG(k.diem) > 7.55;
