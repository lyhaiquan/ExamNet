-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat >= '2024-08-01' AND d.ngay_dat < '2024-09-01' INTERSECT SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat >= '2024-09-01' AND d.ngay_dat < '2024-10-01';

SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat >= '2024-08-01' AND d.ngay_dat < '2024-09-01' AND d.trang_thai <> 'huy' UNION SELECT c.san_pham_id FROM chi_tiet_don c JOIN don_hang d ON d.id = c.don_hang_id WHERE d.ngay_dat >= '2024-09-01' AND d.ngay_dat < '2024-10-01' AND d.trang_thai <> 'huy';
