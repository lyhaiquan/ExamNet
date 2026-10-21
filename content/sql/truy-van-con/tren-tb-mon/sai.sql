-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT sinh_vien_id, mon_hoc_id, lan_thi, diem FROM ket_qua WHERE diem > (SELECT AVG(diem) FROM ket_qua);
