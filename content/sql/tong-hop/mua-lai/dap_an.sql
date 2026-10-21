SELECT id, ho_ten
FROM khach_hang
WHERE id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-01-01' AND '2024-03-31')
  AND id IN (SELECT khach_hang_id FROM don_hang WHERE ngay_dat BETWEEN '2024-04-01' AND '2024-06-30');
