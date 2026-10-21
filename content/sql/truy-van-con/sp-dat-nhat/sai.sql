-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ten, gia FROM san_pham ORDER BY gia DESC LIMIT 1;

SELECT id, ten, MAX(gia) FROM san_pham;
