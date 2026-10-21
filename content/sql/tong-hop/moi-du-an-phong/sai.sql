-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT nv.id, nv.ho_ten FROM nhan_vien nv WHERE NOT EXISTS (SELECT 1 FROM du_an da WHERE da.phong_id = nv.phong_id AND NOT EXISTS (SELECT 1 FROM phan_cong pc WHERE pc.du_an_id = da.id AND pc.nhan_vien_id = nv.id));

SELECT DISTINCT nv.id, nv.ho_ten FROM nhan_vien nv JOIN phan_cong pc ON pc.nhan_vien_id = nv.id JOIN du_an da ON da.id = pc.du_an_id WHERE da.phong_id = nv.phong_id;
