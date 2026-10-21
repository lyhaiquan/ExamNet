SELECT s.id, s.ten, s.ton_kho
FROM san_pham s
WHERE s.ton_kho > 0
  AND NOT EXISTS (
      SELECT 1
      FROM chi_tiet_don c
      JOIN don_hang d ON d.id = c.don_hang_id
      WHERE c.san_pham_id = s.id AND d.trang_thai <> 'huy' AND d.ngay_dat >= date('2024-12-31', '-90 days')
  );
