SELECT DISTINCT s.id, s.ho_ten, m.ten_mon
FROM ket_qua k1
JOIN ket_qua k2 ON k2.sinh_vien_id = k1.sinh_vien_id
               AND k2.mon_hoc_id = k1.mon_hoc_id
               AND k2.lan_thi = 2
JOIN sinh_vien s ON s.id = k1.sinh_vien_id
JOIN mon_hoc m ON m.id = k1.mon_hoc_id
WHERE k1.lan_thi = 1 AND k1.diem < 5 AND k2.diem >= 5;
