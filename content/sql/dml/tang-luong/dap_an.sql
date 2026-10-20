UPDATE nhan_vien
SET luong = luong * 1.1
WHERE phong_id = (SELECT id FROM phong_ban WHERE ten = 'Kỹ thuật');
