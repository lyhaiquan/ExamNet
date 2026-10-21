-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT que_quan FROM sinh_vien WHERE lop_id = 1 UNION SELECT que_quan FROM sinh_vien WHERE lop_id = 2;
