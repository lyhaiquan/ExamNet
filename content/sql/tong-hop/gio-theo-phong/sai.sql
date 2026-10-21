-- Câu sai hay gặp: mỗi câu phải bị chấm sai (SqlLabTest). Server không gửi file này cho app.

SELECT pb.id, pb.ten, SUM(pc.so_gio) AS tong_gio FROM phong_ban pb JOIN du_an da ON da.phong_id = pb.id JOIN phan_cong pc ON pc.du_an_id = da.id GROUP BY pb.id, pb.ten;

SELECT pb.id, pb.ten, SUM(pc.so_gio) AS tong_gio FROM phong_ban pb LEFT JOIN du_an da ON da.phong_id = pb.id LEFT JOIN phan_cong pc ON pc.du_an_id = da.id GROUP BY pb.id, pb.ten;
