SELECT k.id, k.ho_ten
FROM khach_hang k
WHERE NOT EXISTS (
    SELECT 1 FROM danh_muc dm
    WHERE EXISTS (SELECT 1 FROM san_pham sp WHERE sp.danh_muc_id = dm.id)
      AND NOT EXISTS (
          SELECT 1
          FROM don_hang d
          JOIN chi_tiet_don c ON c.don_hang_id = d.id
          JOIN san_pham s ON s.id = c.san_pham_id
          WHERE d.khach_hang_id = k.id AND s.danh_muc_id = dm.id));
