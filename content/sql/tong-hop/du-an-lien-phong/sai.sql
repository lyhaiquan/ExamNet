-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT da.id, da.ten, COUNT(DISTINCT pc.nhan_vien_id) AS so_nv FROM du_an da JOIN phan_cong pc ON pc.du_an_id = da.id JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id WHERE nv.phong_id <> da.phong_id GROUP BY da.id, da.ten ORDER BY so_nv DESC LIMIT 1;

SELECT da.id, da.ten, COUNT(*) AS so_nv FROM du_an da JOIN phan_cong pc ON pc.du_an_id = da.id GROUP BY da.id, da.ten HAVING COUNT(*) = (SELECT COUNT(*) FROM phan_cong GROUP BY du_an_id ORDER BY 1 DESC LIMIT 1);
