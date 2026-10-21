-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten FROM nhan_vien EXCEPT SELECT n.id, n.ho_ten FROM nhan_vien n JOIN phan_cong p ON p.du_an_id = n.id;

SELECT n.id, n.ho_ten FROM nhan_vien n JOIN phan_cong p ON p.nhan_vien_id = n.id;
