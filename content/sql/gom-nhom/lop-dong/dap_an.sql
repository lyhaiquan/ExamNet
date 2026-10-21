SELECT lop_id, COUNT(*) AS si_so
FROM sinh_vien
WHERE lop_id IS NOT NULL
GROUP BY lop_id
HAVING COUNT(*) >= 3;
