-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

UPDATE san_pham SET gia = gia * 0.05 WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện thoại');

UPDATE san_pham SET gia = gia * 0.95 WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện tử');

UPDATE san_pham SET gia = gia - 5 WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện thoại');
