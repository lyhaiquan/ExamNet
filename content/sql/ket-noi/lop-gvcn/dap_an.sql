SELECT l.ten_lop, g.ho_ten
FROM lop l
LEFT JOIN giang_vien g ON g.id = l.gvcn_id;
