SELECT s.id, s.ho_ten,
       ROUND(SUM(m.so_tin_chi * (SELECT MAX(k.diem) FROM ket_qua k
                                 WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id))
             / SUM(m.so_tin_chi), 2) AS diem_tb
FROM sinh_vien s
JOIN mon_hoc m ON EXISTS (SELECT 1 FROM ket_qua k
                          WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id AND k.diem IS NOT NULL)
GROUP BY s.id, s.ho_ten;
