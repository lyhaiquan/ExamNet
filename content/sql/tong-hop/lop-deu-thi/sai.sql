-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT l.id, l.ten_lop FROM lop l WHERE NOT EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.diem IS NOT NULL));

SELECT l.id, l.ten_lop FROM lop l WHERE EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id) AND NOT EXISTS (SELECT 1 FROM sinh_vien s WHERE s.lop_id = l.id AND NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id));

SELECT DISTINCT l.id, l.ten_lop FROM lop l JOIN sinh_vien s ON s.lop_id = l.id JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.diem IS NOT NULL;
