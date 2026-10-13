SELECT k.id, k.ho_ten, COUNT(d.id) AS so_don
FROM khach_hang k
LEFT JOIN don_hang d ON d.khach_hang_id = k.id
GROUP BY k.id, k.ho_ten;
