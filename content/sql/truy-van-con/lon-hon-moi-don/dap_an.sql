SELECT c.don_hang_id, SUM(c.so_luong * c.don_gia) AS tong_tien
FROM chi_tiet_don c
GROUP BY c.don_hang_id
HAVING SUM(c.so_luong * c.don_gia) > COALESCE((
    SELECT SUM(c2.so_luong * c2.don_gia)
    FROM chi_tiet_don c2 JOIN don_hang d2 ON d2.id = c2.don_hang_id
    WHERE d2.khach_hang_id = 2
    GROUP BY c2.don_hang_id
    ORDER BY 1 DESC
    LIMIT 1), 0);
