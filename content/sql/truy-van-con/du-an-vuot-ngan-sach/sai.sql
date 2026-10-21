-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT p.id, p.ten, p.ngan_sach FROM phong_ban p WHERE p.ngan_sach < (SELECT SUM(ngan_sach) FROM du_an);
