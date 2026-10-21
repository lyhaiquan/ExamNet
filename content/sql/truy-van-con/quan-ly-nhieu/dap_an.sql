SELECT id, ho_ten
FROM nhan_vien
WHERE id IN (SELECT quan_ly_id FROM nhan_vien GROUP BY quan_ly_id HAVING COUNT(*) >= 3);
