-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT id, ho_ten, luong, ngay_vao_lam FROM nhan_vien WHERE ngay_vao_lam <= '2020-12-31' AND luong < 20000000;

SELECT id, ho_ten, luong, ngay_vao_lam FROM nhan_vien WHERE ngay_vao_lam < '2020-01-01' AND luong <= 20000000;
