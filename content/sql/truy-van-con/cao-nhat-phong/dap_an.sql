SELECT n.id, n.ho_ten, n.phong_id, n.luong
FROM nhan_vien n
WHERE n.luong = (SELECT MAX(n2.luong) FROM nhan_vien n2 WHERE n2.phong_id = n.phong_id);
