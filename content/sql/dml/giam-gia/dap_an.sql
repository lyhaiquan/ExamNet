UPDATE san_pham
SET gia = gia * 0.95
WHERE danh_muc_id = (SELECT id FROM danh_muc WHERE ten = 'Điện thoại');
