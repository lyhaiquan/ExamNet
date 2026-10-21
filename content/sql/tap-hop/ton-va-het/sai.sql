-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ten FROM san_pham WHERE ton_kho > 0 AND id NOT IN (SELECT san_pham_id FROM chi_tiet_don) UNION SELECT id, ten FROM san_pham WHERE ton_kho = 0;

SELECT id, ten FROM san_pham WHERE ton_kho > 0 EXCEPT SELECT s.id, s.ten FROM san_pham s JOIN chi_tiet_don c ON c.san_pham_id = s.id;
