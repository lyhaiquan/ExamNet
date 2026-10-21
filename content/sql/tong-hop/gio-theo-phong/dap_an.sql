SELECT pb.id, pb.ten, COALESCE(SUM(pc.so_gio), 0) AS tong_gio
FROM phong_ban pb
LEFT JOIN du_an da ON da.phong_id = pb.id
LEFT JOIN phan_cong pc ON pc.du_an_id = da.id
GROUP BY pb.id, pb.ten;
