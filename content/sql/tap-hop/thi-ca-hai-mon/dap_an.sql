SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 1 AND diem IS NOT NULL
INTERSECT
SELECT sinh_vien_id FROM ket_qua WHERE mon_hoc_id = 2 AND diem IS NOT NULL;
