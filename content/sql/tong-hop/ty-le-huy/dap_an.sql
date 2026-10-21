SELECT strftime('%Y-%m', ngay_dat) AS thang,
       ROUND(100.0 * SUM(CASE WHEN trang_thai = 'huy' THEN 1 ELSE 0 END) / COUNT(*), 2) AS ty_le_huy
FROM don_hang
GROUP BY strftime('%Y-%m', ngay_dat);
