SELECT a.id, b.id, a.que_quan
FROM sinh_vien a
JOIN sinh_vien b ON b.que_quan = a.que_quan AND a.id < b.id
WHERE a.lop_id <> b.lop_id;
