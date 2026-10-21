-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT d.id, k.ho_ten, COUNT(*) FROM don_hang d JOIN chi_tiet_don c ON c.don_hang_id = d.id LEFT JOIN khach_hang k ON k.id = d.khach_hang_id GROUP BY d.id, k.ho_ten HAVING COUNT(*) >= 3;
