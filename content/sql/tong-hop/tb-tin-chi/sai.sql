-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT s.id, s.ho_ten, ROUND(AVG(k.diem), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.diem IS NOT NULL GROUP BY s.id, s.ho_ten;

SELECT s.id, s.ho_ten, ROUND(1.0 * SUM(k.diem * m.so_tin_chi) / SUM(m.so_tin_chi), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id JOIN mon_hoc m ON m.id = k.mon_hoc_id WHERE k.diem IS NOT NULL GROUP BY s.id, s.ho_ten;

SELECT s.id, s.ho_ten, ROUND(1.0 * SUM(k.diem * m.so_tin_chi) / SUM(m.so_tin_chi), 2) FROM sinh_vien s JOIN ket_qua k ON k.sinh_vien_id = s.id JOIN mon_hoc m ON m.id = k.mon_hoc_id WHERE k.lan_thi = 1 AND k.diem IS NOT NULL GROUP BY s.id, s.ho_ten;
