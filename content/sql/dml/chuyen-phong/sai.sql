-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

UPDATE nhan_vien SET phong_id = 4 WHERE phong_id = 3;

UPDATE nhan_vien SET phong_id = (SELECT id FROM phong_ban WHERE ten = 'Pháp chế');
