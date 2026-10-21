SELECT dm.id, dm.ten
FROM danh_muc dm
WHERE NOT EXISTS (
    SELECT 1
    FROM san_pham s
    JOIN chi_tiet_don c ON c.san_pham_id = s.id
    JOIN don_hang d ON d.id = c.don_hang_id
    WHERE s.danh_muc_id = dm.id AND d.trang_thai <> 'huy'
      AND d.ngay_dat >= '2024-01-01' AND d.ngay_dat < '2025-01-01'
);
