-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten FROM khach_hang WHERE id NOT IN (SELECT khach_hang_id FROM don_hang);

SELECT k.id, k.ho_ten FROM khach_hang k JOIN don_hang d ON d.khach_hang_id <> k.id;
