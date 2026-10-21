-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT m.ma_mon, m.ten_mon FROM mon_hoc m LEFT JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.diem IS NULL;

SELECT m.ma_mon, m.ten_mon FROM mon_hoc m JOIN ket_qua k ON k.mon_hoc_id = m.id WHERE k.diem IS NULL;
