-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT nv.id, nv.ho_ten FROM nhan_vien nv JOIN nhan_vien ql ON ql.id = nv.quan_ly_id WHERE ql.phong_id <> nv.phong_id;

SELECT nv.id, nv.ho_ten FROM nhan_vien nv JOIN nhan_vien ql ON ql.id = nv.quan_ly_id JOIN nhan_vien ql2 ON ql2.id = ql.quan_ly_id WHERE ql2.phong_id <> ql.phong_id;
