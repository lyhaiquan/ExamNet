-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten FROM sinh_vien WHERE lop_id = (SELECT id FROM lop WHERE khoa = 'CNTT');
