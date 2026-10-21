SELECT k.sinh_vien_id, s.ho_ten, m.ten_mon, MAX(k.diem) AS diem_cao_nhat
FROM ket_qua k
JOIN sinh_vien s ON s.id = k.sinh_vien_id
JOIN mon_hoc m ON m.id = k.mon_hoc_id
GROUP BY k.sinh_vien_id, s.ho_ten, k.mon_hoc_id, m.ten_mon;
