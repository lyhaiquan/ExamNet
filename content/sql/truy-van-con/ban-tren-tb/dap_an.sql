SELECT san_pham_id, SUM(so_luong) AS tong_so_luong
FROM chi_tiet_don
GROUP BY san_pham_id
HAVING SUM(so_luong) > (SELECT SUM(so_luong) * 1.0 / COUNT(DISTINCT san_pham_id) FROM chi_tiet_don);
