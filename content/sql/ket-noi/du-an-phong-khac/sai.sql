-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT n.id, n.ho_ten, d.ten FROM nhan_vien n JOIN phan_cong p ON p.nhan_vien_id = n.id JOIN du_an d ON d.id = p.du_an_id WHERE d.phong_id <> n.phong_id OR d.phong_id IS NULL;

SELECT n.id, n.ho_ten, d.ten FROM nhan_vien n JOIN du_an d ON d.phong_id <> n.phong_id;
