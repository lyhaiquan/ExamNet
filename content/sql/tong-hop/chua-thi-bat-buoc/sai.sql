-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten FROM sinh_vien WHERE id NOT IN (SELECT sinh_vien_id FROM ket_qua);

SELECT s.id, s.ho_ten FROM sinh_vien s WHERE NOT EXISTS (SELECT 1 FROM ket_qua k WHERE k.sinh_vien_id = s.id AND k.mon_hoc_id IN (1, 2));

SELECT s.id, s.ho_ten FROM sinh_vien s LEFT JOIN ket_qua k ON k.sinh_vien_id = s.id WHERE k.sinh_vien_id IS NULL;
