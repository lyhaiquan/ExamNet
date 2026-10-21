SELECT s.ho_ten, m.ten_mon, k.diem
FROM ket_qua k
JOIN sinh_vien s ON s.id = k.sinh_vien_id
JOIN mon_hoc m ON m.id = k.mon_hoc_id;
