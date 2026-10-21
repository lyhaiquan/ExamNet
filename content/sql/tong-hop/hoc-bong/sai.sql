-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY s.id, s.ho_ten HAVING AVG(k.diem) >= 8;

SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY s.id, s.ho_ten HAVING AVG(k.diem) > 8 AND MIN(k.diem) >= 5;
