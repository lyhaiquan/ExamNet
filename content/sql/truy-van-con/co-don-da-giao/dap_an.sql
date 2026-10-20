SELECT k.id, k.ho_ten
FROM khach_hang k
WHERE EXISTS (
    SELECT 1 FROM don_hang d
    WHERE d.khach_hang_id = k.id AND d.trang_thai = 'da-giao'
);
