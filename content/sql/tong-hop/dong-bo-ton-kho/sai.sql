-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

UPDATE san_pham SET ton_kho = ton_kho - (SELECT SUM(c.so_luong) FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = san_pham.id AND d.ngay_dat = '2024-09-20') WHERE id IN (SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat = '2024-09-20');

UPDATE san_pham SET ton_kho = ton_kho - (SELECT c.so_luong FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE c.san_pham_id = san_pham.id AND d.ngay_dat = '2024-09-20' AND d.trang_thai = 'da-giao') WHERE id IN (SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat = '2024-09-20' AND d.trang_thai = 'da-giao');
