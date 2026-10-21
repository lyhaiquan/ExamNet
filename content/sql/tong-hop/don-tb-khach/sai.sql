-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT k.id, k.ho_ten, ROUND(AVG(c.so_luong * c.don_gia), 2) AS gia_tri_tb FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten HAVING COUNT(DISTINCT d.id) >= 2;

SELECT k.id, k.ho_ten, ROUND(1.0 * SUM(c.so_luong * c.don_gia) / COUNT(DISTINCT d.id), 2) AS gia_tri_tb FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.trang_thai <> 'huy' GROUP BY k.id, k.ho_ten HAVING COUNT(*) >= 2;
