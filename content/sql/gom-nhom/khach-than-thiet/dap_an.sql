SELECT khach_hang_id, COUNT(*) AS so_don
FROM don_hang
WHERE khach_hang_id IS NOT NULL
GROUP BY khach_hang_id
HAVING COUNT(*) >= 3;
