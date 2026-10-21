SELECT s.ten, d.ten
FROM san_pham s
JOIN danh_muc d ON d.id = s.danh_muc_id;
