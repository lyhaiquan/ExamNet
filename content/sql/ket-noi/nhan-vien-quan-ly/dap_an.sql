SELECT nv.ho_ten, ql.ho_ten AS quan_ly
FROM nhan_vien nv
JOIN nhan_vien ql ON nv.quan_ly_id = ql.id;
