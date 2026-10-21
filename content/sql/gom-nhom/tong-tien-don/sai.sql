-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT don_hang_id, SUM(don_gia) FROM chi_tiet_don GROUP BY don_hang_id;

SELECT don_hang_id, SUM(so_luong) * SUM(don_gia) FROM chi_tiet_don GROUP BY don_hang_id;
