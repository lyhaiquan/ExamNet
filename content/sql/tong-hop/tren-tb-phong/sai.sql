-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT nv.id, nv.ho_ten, nv.phong_id, nv.luong - (SELECT AVG(luong) FROM nhan_vien) AS chenh FROM nhan_vien nv WHERE nv.luong > (SELECT AVG(luong) FROM nhan_vien);

SELECT nv.id, nv.ho_ten, nv.phong_id, nv.luong - (SELECT AVG(n2.luong) FROM nhan_vien n2 WHERE n2.phong_id = nv.phong_id) AS chenh FROM nhan_vien nv WHERE nv.luong >= (SELECT AVG(n2.luong) FROM nhan_vien n2 WHERE n2.phong_id = nv.phong_id);
