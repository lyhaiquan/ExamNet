-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ten FROM san_pham ORDER BY ten, id LIMIT 3 OFFSET 4;

SELECT id, ten FROM san_pham ORDER BY id LIMIT 3 OFFSET 3;
