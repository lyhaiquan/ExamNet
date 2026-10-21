SELECT l.khoa, s.que_quan, COUNT(*) AS so_sv
FROM sinh_vien s
JOIN lop l ON l.id = s.lop_id
WHERE s.que_quan IS NOT NULL
GROUP BY l.khoa, s.que_quan
HAVING COUNT(*) = (
    SELECT COUNT(*)
    FROM sinh_vien s2
    JOIN lop l2 ON l2.id = s2.lop_id
    WHERE l2.khoa = l.khoa AND s2.que_quan IS NOT NULL
    GROUP BY s2.que_quan
    ORDER BY COUNT(*) DESC
    LIMIT 1
);
