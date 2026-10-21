-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT pb.id, pb.ten, SUM(nv.luong) AS tong_luong_nam, pb.ngan_sach FROM phong_ban pb JOIN nhan_vien nv ON nv.phong_id = pb.id GROUP BY pb.id, pb.ten, pb.ngan_sach HAVING SUM(nv.luong) > pb.ngan_sach;

SELECT pb.id, pb.ten, SUM(nv.luong) * 12 AS tong_luong_nam, pb.ngan_sach FROM phong_ban pb JOIN nhan_vien nv ON nv.phong_id = pb.id GROUP BY pb.id, pb.ten, pb.ngan_sach;
