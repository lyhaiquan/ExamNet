SELECT strftime('%Y', ngay_vao_lam) AS nam, COUNT(*) AS so_nv, ROUND(AVG(luong), 2) AS luong_tb
FROM nhan_vien
GROUP BY strftime('%Y', ngay_vao_lam);
