SELECT ho_ten, luong
FROM nhan_vien
WHERE luong > (SELECT AVG(luong) FROM nhan_vien);
