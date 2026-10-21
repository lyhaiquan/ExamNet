SELECT id, ten FROM san_pham
WHERE ton_kho > 0 AND id NOT IN (SELECT san_pham_id FROM chi_tiet_don)
UNION
SELECT id, ten FROM san_pham
WHERE ton_kho = 0 AND id IN (SELECT san_pham_id FROM chi_tiet_don);
