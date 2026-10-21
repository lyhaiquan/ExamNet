SELECT n.id, n.ho_ten, d.ten
FROM nhan_vien n
JOIN phan_cong p ON p.nhan_vien_id = n.id
JOIN du_an d ON d.id = p.du_an_id
WHERE d.phong_id <> n.phong_id;
