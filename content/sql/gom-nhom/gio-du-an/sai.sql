-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT du_an_id, SUM(so_gio) FROM phan_cong GROUP BY du_an_id HAVING SUM(so_gio) > 100;

SELECT du_an_id, SUM(so_gio) FROM phan_cong WHERE so_gio >= 100 GROUP BY du_an_id;
