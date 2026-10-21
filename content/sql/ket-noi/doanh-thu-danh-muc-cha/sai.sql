-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT g.ten, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id JOIN san_pham s ON s.id = c.san_pham_id JOIN danh_muc m ON m.id = s.danh_muc_id JOIN danh_muc g ON g.id = m.cha_id WHERE d.trang_thai = 'da-giao' GROUP BY g.id, g.ten;

SELECT g.ten, SUM(c.so_luong * c.don_gia) FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id JOIN san_pham s ON s.id = c.san_pham_id JOIN danh_muc m ON m.id = s.danh_muc_id JOIN danh_muc g ON g.id = COALESCE(m.cha_id, m.id) GROUP BY g.id, g.ten;
