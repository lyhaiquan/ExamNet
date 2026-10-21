SELECT n.ho_ten, d.ten, p.so_gio
FROM phan_cong p
JOIN nhan_vien n ON n.id = p.nhan_vien_id
JOIN du_an d ON d.id = p.du_an_id;
