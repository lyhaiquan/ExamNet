-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT l.ten_lop, g.ho_ten FROM lop l JOIN giang_vien g ON g.id = l.gvcn_id;

SELECT l.ten_lop, g.ho_ten FROM giang_vien g LEFT JOIN lop l ON g.id = l.gvcn_id;
