-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT s.danh_muc_id, s.id, s.ten, SUM(c.so_luong) AS so_luong FROM san_pham s JOIN chi_tiet_don c ON c.san_pham_id = s.id GROUP BY s.danh_muc_id, s.id, s.ten HAVING SUM(c.so_luong) = (SELECT SUM(c2.so_luong) FROM san_pham s2 JOIN chi_tiet_don c2 ON c2.san_pham_id = s2.id WHERE s2.danh_muc_id = s.danh_muc_id GROUP BY s2.id ORDER BY 1 DESC LIMIT 1);

SELECT s.danh_muc_id, s.id, s.ten, COUNT(*) AS so_luong FROM san_pham s JOIN chi_tiet_don c ON c.san_pham_id = s.id JOIN don_hang d ON d.id = c.don_hang_id WHERE d.trang_thai <> 'huy' GROUP BY s.danh_muc_id, s.id, s.ten HAVING COUNT(*) = (SELECT COUNT(*) FROM san_pham s2 JOIN chi_tiet_don c2 ON c2.san_pham_id = s2.id JOIN don_hang d2 ON d2.id = c2.don_hang_id WHERE s2.danh_muc_id = s.danh_muc_id AND d2.trang_thai <> 'huy' GROUP BY s2.id ORDER BY 1 DESC LIMIT 1);
