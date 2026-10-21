DELETE FROM khach_hang
WHERE ngay_dang_ky < '2024-07-01'
  AND id NOT IN (SELECT khach_hang_id FROM don_hang WHERE khach_hang_id IS NOT NULL);
