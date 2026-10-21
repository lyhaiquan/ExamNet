-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) AS tong_tien FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id GROUP BY k.id, k.ho_ten ORDER BY tong_tien DESC, k.id LIMIT 3;

SELECT k.id, k.ho_ten, SUM(c.so_luong * c.don_gia) AS tong_tien FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten ORDER BY tong_tien, k.id LIMIT 3;
