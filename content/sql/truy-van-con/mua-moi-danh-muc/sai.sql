-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT k.id, k.ho_ten FROM khach_hang k WHERE NOT EXISTS (SELECT 1 FROM danh_muc dm WHERE NOT EXISTS (SELECT 1 FROM don_hang d JOIN chi_tiet_don c ON c.don_hang_id = d.id JOIN san_pham s ON s.id = c.san_pham_id WHERE d.khach_hang_id = k.id AND s.danh_muc_id = dm.id));

SELECT DISTINCT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id;
