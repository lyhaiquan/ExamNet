SELECT k.sinh_vien_id, k.mon_hoc_id, k.lan_thi, k.diem
FROM ket_qua k
WHERE k.diem > (SELECT AVG(k2.diem) FROM ket_qua k2 WHERE k2.mon_hoc_id = k.mon_hoc_id);
