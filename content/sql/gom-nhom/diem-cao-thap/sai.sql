-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT mon_hoc_id, MAX(diem), MIN(diem) FROM ket_qua;

SELECT sinh_vien_id, MAX(diem), MIN(diem) FROM ket_qua GROUP BY sinh_vien_id;
