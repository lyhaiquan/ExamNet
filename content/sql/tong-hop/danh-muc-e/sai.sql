-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT dm.id, dm.ten FROM danh_muc dm WHERE NOT EXISTS (SELECT 1 FROM san_pham s JOIN chi_tiet_don c ON c.san_pham_id = s.id JOIN don_hang d ON d.id = c.don_hang_id WHERE s.danh_muc_id = dm.id AND d.ngay_dat >= '2024-01-01' AND d.ngay_dat < '2025-01-01');

SELECT dm.id, dm.ten FROM danh_muc dm LEFT JOIN san_pham s ON s.danh_muc_id = dm.id WHERE s.id IS NULL;
