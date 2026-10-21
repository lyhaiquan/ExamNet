SELECT phong_id, AVG(luong) AS luong_tb
FROM nhan_vien
WHERE phong_id IS NOT NULL
GROUP BY phong_id
HAVING AVG(luong) > 20000000;
