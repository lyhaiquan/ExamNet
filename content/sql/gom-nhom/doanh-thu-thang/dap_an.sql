SELECT strftime('%m', d.ngay_dat) AS thang, SUM(c.so_luong * c.don_gia) AS doanh_thu
FROM don_hang d
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai <> 'huy' AND strftime('%Y', d.ngay_dat) = '2024'
GROUP BY strftime('%m', d.ngay_dat)
ORDER BY thang;
