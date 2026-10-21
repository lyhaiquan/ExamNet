SELECT d.id, d.ten
FROM du_an d
LEFT JOIN phan_cong p ON p.du_an_id = d.id
WHERE p.du_an_id IS NULL;
