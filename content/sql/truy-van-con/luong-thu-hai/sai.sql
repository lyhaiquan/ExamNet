-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT luong FROM nhan_vien ORDER BY luong DESC LIMIT 1 OFFSET 1;

SELECT MAX(luong) FROM nhan_vien WHERE luong <= (SELECT MAX(luong) FROM nhan_vien);
