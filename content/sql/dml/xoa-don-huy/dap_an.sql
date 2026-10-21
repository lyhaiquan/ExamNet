DELETE FROM chi_tiet_don
WHERE don_hang_id IN (SELECT id FROM don_hang WHERE trang_thai = 'huy');
