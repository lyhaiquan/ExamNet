SELECT id, ho_ten FROM nhan_vien
EXCEPT
SELECT n.id, n.ho_ten FROM nhan_vien n JOIN phan_cong p ON p.nhan_vien_id = n.id;
