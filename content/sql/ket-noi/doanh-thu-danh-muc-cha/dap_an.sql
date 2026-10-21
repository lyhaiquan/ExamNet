SELECT g.ten, SUM(c.so_luong * c.don_gia) AS doanh_thu
FROM chi_tiet_don c
JOIN don_hang d ON d.id = c.don_hang_id
JOIN san_pham s ON s.id = c.san_pham_id
JOIN danh_muc m ON m.id = s.danh_muc_id
JOIN danh_muc g ON g.id = COALESCE(m.cha_id, m.id)
WHERE d.trang_thai = 'da-giao'
GROUP BY g.id, g.ten;
