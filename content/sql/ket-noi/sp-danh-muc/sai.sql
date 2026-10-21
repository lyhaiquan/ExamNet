-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT s.ten, d.ten FROM san_pham s JOIN danh_muc d ON d.id = s.id;

SELECT s.ten, d.ten FROM san_pham s JOIN danh_muc d ON d.cha_id = s.danh_muc_id;
