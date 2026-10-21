SELECT nv.id, nv.ho_ten, nv.phong_id, nv.luong - AVG(n2.luong) AS chenh
FROM nhan_vien nv
JOIN nhan_vien n2 ON n2.phong_id = nv.phong_id
GROUP BY nv.id, nv.ho_ten, nv.phong_id, nv.luong
HAVING nv.luong > AVG(n2.luong);
