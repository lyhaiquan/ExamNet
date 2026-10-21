-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT c.don_hang_id, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c GROUP BY c.don_hang_id HAVING SUM(c.so_luong * c.don_gia) > (SELECT SUM(c2.so_luong * c2.don_gia) FROM chi_tiet_don c2 JOIN don_hang d2 ON d2.id = c2.don_hang_id WHERE d2.khach_hang_id = 2 GROUP BY c2.don_hang_id ORDER BY 1 LIMIT 1);

SELECT c.don_hang_id, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c GROUP BY c.don_hang_id HAVING SUM(c.so_luong * c.don_gia) > (SELECT MAX(c2.so_luong * c2.don_gia) FROM chi_tiet_don c2 JOIN don_hang d2 ON d2.id = c2.don_hang_id WHERE d2.khach_hang_id = 2);
