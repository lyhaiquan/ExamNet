-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT nv.id, nv.ho_ten, nv.luong, ql.luong FROM nhan_vien nv JOIN nhan_vien ql ON ql.id = nv.quan_ly_id WHERE nv.luong >= ql.luong;

SELECT nv.id, nv.ho_ten, nv.luong, ql.luong FROM nhan_vien nv JOIN nhan_vien ql ON nv.id = ql.quan_ly_id WHERE nv.luong > ql.luong;
