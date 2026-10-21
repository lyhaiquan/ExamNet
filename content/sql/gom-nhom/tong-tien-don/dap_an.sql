SELECT don_hang_id, SUM(so_luong * don_gia) AS tong_tien
FROM chi_tiet_don
GROUP BY don_hang_id;
