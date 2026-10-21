-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT strftime('%m', d.ngay_dat), SUM(c.so_luong * c.don_gia) FROM don_hang d JOIN chi_tiet_don c ON c.don_hang_id = d.id GROUP BY strftime('%m', d.ngay_dat) ORDER BY strftime('%m', d.ngay_dat);

SELECT strftime('%m', d.ngay_dat), SUM(c.don_gia) FROM don_hang d JOIN chi_tiet_don c ON c.don_hang_id = d.id WHERE d.trang_thai <> 'huy' GROUP BY strftime('%m', d.ngay_dat) ORDER BY strftime('%m', d.ngay_dat);
