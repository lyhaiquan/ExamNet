SELECT k.thanh_pho, SUM(c.so_luong * c.don_gia) AS doanh_thu
FROM don_hang d
JOIN khach_hang k ON k.id = d.khach_hang_id
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai = 'da-giao' AND k.thanh_pho IS NOT NULL
GROUP BY k.thanh_pho
ORDER BY doanh_thu DESC, k.thanh_pho;
