SELECT nv.id, nv.ho_ten
FROM nhan_vien nv
JOIN nhan_vien ql ON ql.id = nv.quan_ly_id
JOIN nhan_vien ql2 ON ql2.id = ql.quan_ly_id
WHERE ql2.phong_id <> nv.phong_id;
