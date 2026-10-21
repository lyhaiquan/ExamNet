-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT da.id, da.ten, SUM(pc.so_gio * nv.luong / 160.0) AS chi_phi FROM du_an da JOIN phan_cong pc ON pc.du_an_id = da.id JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id GROUP BY da.id, da.ten;

SELECT da.id, da.ten, COALESCE(SUM(pc.so_gio * nv.luong), 0) AS chi_phi FROM du_an da LEFT JOIN phan_cong pc ON pc.du_an_id = da.id LEFT JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id GROUP BY da.id, da.ten;
