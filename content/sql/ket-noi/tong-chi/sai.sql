-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy' LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id GROUP BY k.id, k.ho_ten;

SELECT k.id, k.ho_ten, COALESCE(SUM(c.so_luong * c.don_gia), 0) FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten;

SELECT k.id, k.ho_ten, COALESCE(SUM(c.so_luong * c.don_gia), 0) FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id LEFT JOIN chi_tiet_don c ON c.don_hang_id = d.id GROUP BY k.id, k.ho_ten;
