-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

DELETE FROM khach_hang WHERE ngay_dang_ky < '2024-07-01' AND id NOT IN (SELECT khach_hang_id FROM don_hang);

DELETE FROM khach_hang WHERE id NOT IN (SELECT khach_hang_id FROM don_hang WHERE khach_hang_id IS NOT NULL);
