-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT DISTINCT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE c.san_pham_id IN (1, 2);

SELECT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id = k.id JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE c.san_pham_id IN (1, 2) GROUP BY k.id, k.ho_ten HAVING COUNT(*) = 2;
