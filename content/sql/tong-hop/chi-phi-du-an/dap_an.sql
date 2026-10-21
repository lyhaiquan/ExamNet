SELECT da.id, da.ten, COALESCE(SUM(pc.so_gio * nv.luong / 160.0), 0) AS chi_phi
FROM du_an da
LEFT JOIN phan_cong pc ON pc.du_an_id = da.id
LEFT JOIN nhan_vien nv ON nv.id = pc.nhan_vien_id
GROUP BY da.id, da.ten;
