SELECT s.id, s.ten
FROM san_pham s
LEFT JOIN chi_tiet_don c ON c.san_pham_id = s.id
WHERE c.san_pham_id IS NULL;
