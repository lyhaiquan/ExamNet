-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT a.id, b.id, a.phong_id FROM nhan_vien a JOIN nhan_vien b ON b.phong_id = a.phong_id AND a.id <> b.id;

SELECT a.id, b.id, a.phong_id FROM nhan_vien a JOIN nhan_vien b ON b.phong_id = a.phong_id;
