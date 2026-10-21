-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT m.id, m.ten_mon FROM mon_hoc m WHERE EXISTS (SELECT 1 FROM ket_qua k JOIN sinh_vien s ON s.id = k.sinh_vien_id WHERE s.lop_id = 1 AND k.mon_hoc_id = m.id);

SELECT m.id, m.ten_mon FROM mon_hoc m WHERE NOT EXISTS (SELECT 1 FROM sinh_vien s WHERE NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id = m.id));
