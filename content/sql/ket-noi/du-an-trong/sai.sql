-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT d.id, d.ten FROM du_an d JOIN phan_cong p ON p.du_an_id = d.id WHERE p.nhan_vien_id IS NULL;

SELECT d.id, d.ten FROM du_an d LEFT JOIN phan_cong p ON p.du_an_id = d.id WHERE d.phong_id IS NULL;
