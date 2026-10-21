SELECT d.khach_hang_id, d.id, SUM(c.so_luong * c.don_gia) AS gia_tri
FROM don_hang d
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.khach_hang_id IS NOT NULL AND d.trang_thai <> 'huy'
GROUP BY d.khach_hang_id, d.id
HAVING SUM(c.so_luong * c.don_gia) = (
    SELECT SUM(c2.so_luong * c2.don_gia)
    FROM don_hang d2
    JOIN chi_tiet_don c2 ON c2.don_hang_id = d2.id
    WHERE d2.khach_hang_id = d.khach_hang_id AND d2.trang_thai <> 'huy'
    GROUP BY d2.id
    ORDER BY 1 DESC
    LIMIT 1
);
