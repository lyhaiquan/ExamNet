INSERT INTO hoc_bong (sinh_vien_id, ho_ten, diem)
SELECT s.id, s.ho_ten, k.diem
FROM sinh_vien s
JOIN ket_qua k ON k.sinh_vien_id = s.id
WHERE k.mon_hoc_id = 1 AND k.lan_thi = 1 AND k.diem >= 8;
