SELECT m.id, m.ten_mon
FROM mon_hoc m
WHERE NOT EXISTS (
    SELECT 1 FROM sinh_vien s
    WHERE s.lop_id = 1
      AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id));
