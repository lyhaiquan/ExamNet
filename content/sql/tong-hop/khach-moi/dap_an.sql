SELECT strftime('%Y-%m', k.ngay_dang_ky) AS thang,
       COUNT(DISTINCT k.id) AS khach_moi,
       COUNT(DISTINCT d.khach_hang_id) AS da_mua
FROM khach_hang k
LEFT JOIN don_hang d ON d.khach_hang_id = k.id AND d.trang_thai <> 'huy'
GROUP BY strftime('%Y-%m', k.ngay_dang_ky);
