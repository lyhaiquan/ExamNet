SELECT s.ho_ten, l.ten_lop
FROM sinh_vien s
JOIN lop l ON s.lop_id = l.id;
