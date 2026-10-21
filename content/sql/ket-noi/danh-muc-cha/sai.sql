-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT con.ten, cha.ten FROM danh_muc con JOIN danh_muc cha ON cha.id = con.cha_id;

SELECT con.ten, cha.ten FROM danh_muc con LEFT JOIN danh_muc cha ON con.id = cha.cha_id;
