SELECT l.ten_lop, COUNT(*) AS si_so
FROM sinh_vien s
JOIN lop l ON s.lop_id = l.id
GROUP BY l.ten_lop;
