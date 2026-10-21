SELECT id, ho_ten
FROM sinh_vien
WHERE lop_id IN (SELECT id FROM lop WHERE khoa = 'CNTT');
