-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT ngay_vao_lam, COUNT(*), ROUND(AVG(luong), 2) FROM nhan_vien GROUP BY ngay_vao_lam;

SELECT strftime('%Y', ngay_vao_lam) AS nam, COUNT(*), SUM(luong) FROM nhan_vien GROUP BY strftime('%Y', ngay_vao_lam);
