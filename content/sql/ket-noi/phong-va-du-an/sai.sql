-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT p.ten, d.ten FROM phong_ban p LEFT JOIN du_an d ON d.phong_id = p.id;

SELECT p.ten, d.ten FROM phong_ban p JOIN du_an d ON d.phong_id = p.id;
