SELECT s.id, s.ho_ten
FROM sinh_vien s
WHERE NOT EXISTS (
    SELECT 1
    FROM ket_qua k
    WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id IN (1, 2) AND k.diem IS NOT NULL
);
