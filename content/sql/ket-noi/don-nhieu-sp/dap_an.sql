SELECT d.id, k.ho_ten, COUNT(DISTINCT c.san_pham_id) AS so_san_pham
FROM don_hang d
JOIN chi_tiet_don c ON c.don_hang_id = d.id
LEFT JOIN khach_hang k ON k.id = d.khach_hang_id
GROUP BY d.id, k.ho_ten
HAVING COUNT(DISTINCT c.san_pham_id) >= 3;
