-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, khach_hang_id, ngay_dat FROM don_hang WHERE ngay_dat > '2024-09-01' AND ngay_dat < '2024-10-01';

SELECT id, khach_hang_id, ngay_dat FROM don_hang WHERE ngay_dat < '2024-10-01';
