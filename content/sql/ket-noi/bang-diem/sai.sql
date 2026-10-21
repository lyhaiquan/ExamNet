-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT s.ho_ten, m.ten_mon, k.diem FROM ket_qua k JOIN sinh_vien s ON s.id = k.sinh_vien_id JOIN mon_hoc m ON m.id = k.sinh_vien_id;

SELECT s.ho_ten, m.ten_mon, k.diem FROM ket_qua k JOIN sinh_vien s ON s.id = k.sinh_vien_id JOIN mon_hoc m ON m.id = k.mon_hoc_id WHERE k.lan_thi = 1;
