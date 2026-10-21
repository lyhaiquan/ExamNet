-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT s.id, s.ten FROM san_pham s JOIN chi_tiet_don c ON c.san_pham_id = s.id WHERE c.so_luong IS NULL;

SELECT s.id, s.ten FROM san_pham s WHERE s.ton_kho = 0;
