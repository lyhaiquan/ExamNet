-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT san_pham_id, SUM(so_luong) FROM chi_tiet_don GROUP BY san_pham_id HAVING SUM(so_luong) > (SELECT AVG(so_luong) FROM chi_tiet_don);
