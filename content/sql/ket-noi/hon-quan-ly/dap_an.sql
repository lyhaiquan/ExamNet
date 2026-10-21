SELECT nv.id, nv.ho_ten, nv.luong, ql.luong
FROM nhan_vien nv
JOIN nhan_vien ql ON ql.id = nv.quan_ly_id
WHERE nv.luong > ql.luong;
