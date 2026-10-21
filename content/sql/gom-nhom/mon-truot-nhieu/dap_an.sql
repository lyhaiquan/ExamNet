SELECT mon_hoc_id, COUNT(*) AS so_lan
FROM ket_qua
WHERE diem < 4
GROUP BY mon_hoc_id
HAVING COUNT(*) = (
    SELECT COUNT(*) FROM ket_qua WHERE diem < 4
    GROUP BY mon_hoc_id ORDER BY COUNT(*) DESC LIMIT 1
);
