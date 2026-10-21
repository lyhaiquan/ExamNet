-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

UPDATE san_pham SET ton_kho = ton_kho - 1 WHERE id IN (SELECT san_pham_id FROM chi_tiet_don WHERE don_hang_id = 4);

UPDATE san_pham SET ton_kho = ton_kho - 2 WHERE id = 3;
