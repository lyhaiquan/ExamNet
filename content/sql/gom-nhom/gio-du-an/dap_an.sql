SELECT du_an_id, SUM(so_gio) AS tong_gio
FROM phan_cong
GROUP BY du_an_id
HAVING SUM(so_gio) >= 100;
