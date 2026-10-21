UPDATE nhan_vien
SET phong_id = (SELECT id FROM phong_ban WHERE ten = 'Pháp chế')
WHERE phong_id = (SELECT id FROM phong_ban WHERE ten = 'Nhân sự');
