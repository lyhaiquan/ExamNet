SELECT d.id, d.ngay_dat, k.ho_ten
FROM don_hang d
JOIN khach_hang k ON k.id = d.khach_hang_id
WHERE k.thanh_pho = 'Hà Nội';
