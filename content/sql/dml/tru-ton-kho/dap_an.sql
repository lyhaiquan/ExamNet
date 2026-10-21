UPDATE san_pham
SET ton_kho = ton_kho - (SELECT SUM(so_luong) FROM chi_tiet_don c
                         WHERE c.don_hang_id = 4 AND c.san_pham_id = san_pham.id)
WHERE id IN (SELECT san_pham_id FROM chi_tiet_don WHERE don_hang_id = 4);
