SELECT p.id, p.ten, p.ngan_sach
FROM phong_ban p
WHERE p.ngan_sach < (SELECT SUM(d.ngan_sach) FROM du_an d WHERE d.phong_id = p.id);
