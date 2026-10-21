-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ten, gia FROM san_pham WHERE gia > 100000 AND gia < 500000 AND ton_kho > 0;

SELECT id, ten, gia FROM san_pham WHERE gia BETWEEN 100000 AND 500000;
