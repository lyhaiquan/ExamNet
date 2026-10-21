-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT COUNT(*), COUNT(diem), COUNT(diem IS NULL) FROM ket_qua;

SELECT COUNT(*), COUNT(*), 0 FROM ket_qua;
