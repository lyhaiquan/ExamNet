SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) AS tong_tien
FROM khach_hang k
JOIN don_hang d ON d.khach_hang_id = k.id
JOIN chi_tiet_don c ON c.don_hang_id = d.id
WHERE d.trang_thai <> 'huy' AND d.ngay_dat >= '2024-01-01' AND d.ngay_dat < '2025-01-01'
GROUP BY k.id, k.ho_ten
ORDER BY tong_tien DESC, k.id
LIMIT 3;
