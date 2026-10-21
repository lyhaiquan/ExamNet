SELECT COALESCE(p.ten, 'Chưa phân') AS phong, COUNT(*) AS so_nhan_vien
FROM nhan_vien n
LEFT JOIN phong_ban p ON p.id = n.phong_id
GROUP BY n.phong_id, p.ten;
