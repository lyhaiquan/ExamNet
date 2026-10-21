SELECT da.id, da.ten, COUNT(DISTINCT pc.nhan_vien_id) AS so_nv
FROM du_an da
JOIN phan_cong pc ON pc.du_an_id = da.id
JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id
WHERE nv.phong_id <> da.phong_id
GROUP BY da.id, da.ten
HAVING COUNT(DISTINCT pc.nhan_vien_id) = (
    SELECT COUNT(DISTINCT pc2.nhan_vien_id)
    FROM du_an d2
    JOIN phan_cong pc2 ON pc2.du_an_id = d2.id
    JOIN nhan_vien n2 ON n2.id = pc2.nhan_vien_id
    WHERE n2.phong_id <> d2.phong_id
    GROUP BY d2.id
    ORDER BY 1 DESC
    LIMIT 1
);
