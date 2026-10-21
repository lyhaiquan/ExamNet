-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT strftime('%Y-%m', k.ngay_dang_ky) AS thang, COUNT(DISTINCT k.id), COUNT(DISTINCT d.khach_hang_id) FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id WHERE d.trang_thai <> 'huy' GROUP BY strftime('%Y-%m', k.ngay_dang_ky);

SELECT strftime('%Y-%m', k.ngay_dang_ky) AS thang, COUNT(*), COUNT(d.khach_hang_id) FROM khach_hang k LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy' GROUP BY strftime('%Y-%m', k.ngay_dang_ky);
