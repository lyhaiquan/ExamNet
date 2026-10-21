SELECT l.id, l.ten_lop
FROM lop l
WHERE EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id)
  AND NOT EXISTS (
      SELECT 1
      FROM sinh_vien s
      WHERE s.lop_id = l.id
        AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.diem IS NOT NULL)
  );
