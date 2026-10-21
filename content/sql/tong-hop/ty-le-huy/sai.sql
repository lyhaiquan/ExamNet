-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT strftime('%Y-%m', ngay_dat) AS thang, SUM(CASE WHEN trang_thai = 'huy' THEN 1 ELSE 0 END) / COUNT(*) * 100 AS ty_le_huy FROM don_hang GROUP BY strftime('%Y-%m', ngay_dat);

SELECT strftime('%Y-%m', ngay_dat) AS thang, ROUND(100.0 * COUNT(trang_thai = 'huy') / COUNT(*), 2) AS ty_le_huy FROM don_hang GROUP BY strftime('%Y-%m', ngay_dat);
