SELECT MAX(luong) AS luong_thu_hai
FROM nhan_vien
WHERE luong < (SELECT MAX(luong) FROM nhan_vien);
