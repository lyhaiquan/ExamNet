SELECT p.ten, d.ten
FROM phong_ban p
FULL OUTER JOIN du_an d ON d.phong_id = p.id;
