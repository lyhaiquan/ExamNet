SELECT danh_muc_id, COUNT(*) AS so_san_pham
FROM san_pham
GROUP BY danh_muc_id;
