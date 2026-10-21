-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ten, CASE WHEN ton_kho < 10 THEN 'Ít' WHEN ton_kho = 0 THEN 'Hết' ELSE 'Nhiều' END FROM san_pham;

SELECT id, ten, CASE WHEN ton_kho = 0 THEN 'Hết' WHEN ton_kho <= 10 THEN 'Ít' ELSE 'Nhiều' END FROM san_pham;
