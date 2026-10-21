SELECT id, ho_ten FROM giang_vien WHERE luong > 20000000
INTERSECT
SELECT g.id, g.ho_ten FROM giang_vien g JOIN lop l ON l.gvcn_id = g.id;
