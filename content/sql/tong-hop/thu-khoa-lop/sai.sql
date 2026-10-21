-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT l.ten_lop, s.id, s.ho_ten, ROUND(AVG(k.diem), 2) AS diem_tb FROM sinh_vien s JOIN lop l ON l.id = s.lop_id JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY l.id, l.ten_lop, s.id, s.ho_ten ORDER BY diem_tb DESC LIMIT 1;

SELECT l.ten_lop, s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN lop l ON l.id = s.lop_id JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY l.id, l.ten_lop, s.id, s.ho_ten HAVING AVG(k.diem) = (SELECT AVG(k2.diem) FROM sinh_vien s2 JOIN ket_qua k2 ON k2.sinh_vien_id = s2.id WHERE s2.lop_id IS NOT NULL AND k2.lan_thi = 1 AND k2.diem IS NOT NULL GROUP BY s2.id ORDER BY AVG(k2.diem) DESC LIMIT 1);
