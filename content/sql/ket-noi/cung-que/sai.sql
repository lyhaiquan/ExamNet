-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT a.id, b.id, a.que_quan FROM sinh_vien a JOIN sinh_vien b ON b.que_quan = a.que_quan AND a.id <> b.id WHERE a.lop_id <> b.lop_id;

SELECT a.id, b.id, a.que_quan FROM sinh_vien a JOIN sinh_vien b ON b.que_quan = a.que_quan AND a.id < b.id WHERE a.lop_id <> b.lop_id OR a.lop_id IS NULL;
