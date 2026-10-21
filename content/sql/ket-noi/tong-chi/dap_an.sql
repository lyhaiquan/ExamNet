SELECT k.id, k.ho_ten, COALESCE(SUM(c.so_luong * c.don_gia), 0) AS tong_chi
FROM khach_hang k
LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy'
LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id
GROUP BY k.id, k.ho_ten;
