SELECT p.id, p.ten, COUNT(*) AS so_nhan_vien
FROM phong_ban p JOIN nhan_vien n ON n.phong_id = p.id
GROUP BY p.id, p.ten
HAVING COUNT(*) = (
    SELECT COUNT(*) FROM nhan_vien
    WHERE phong_id IS NOT NULL
    GROUP BY phong_id
    ORDER BY 1 DESC
    LIMIT 1);
