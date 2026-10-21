SELECT m.ma_mon, m.ten_mon
FROM mon_hoc m
LEFT JOIN ket_qua k ON k.mon_hoc_id = m.id
WHERE k.mon_hoc_id IS NULL;
