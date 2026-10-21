-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT d.id, d.ngay_dat, k.ho_ten FROM don_hang d JOIN khach_hang k ON k.id = d.khach_hang_id;

SELECT d.id, d.ngay_dat, k.ho_ten FROM don_hang d JOIN khach_hang k ON k.id = d.id WHERE k.thanh_pho = 'Hà Nội';
