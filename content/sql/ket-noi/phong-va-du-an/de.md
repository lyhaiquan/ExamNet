# Ghép phòng và dự án, giữ cả hai phía

Ghép **tên phòng** với **tên dự án** của phòng đó. Giữ **cả hai phía**: phòng chưa có dự án nào vẫn hiện (tên dự án
NULL), dự án chưa giao phòng nào vẫn hiện (tên phòng NULL).

`FULL OUTER JOIN` = `LEFT JOIN` cộng thêm các dòng bên phải không ghép được. SQLite từ bản 3.39 có `FULL JOIN`;
cách khác là `LEFT JOIN … UNION ALL` phần dự án không có phòng.

Kết quả có 2 cột: ten, ten. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`phong_ban(id, ten, ngan_sach)`

| id | ten | ngan_sach |
| --- | --- | --- |
| 1 | Kỹ thuật | 500000000 |
| 2 | Kinh doanh | 300000000 |
| 3 | Nhân sự | 150000000 |
| 4 | Pháp chế | 100000000 |

`du_an(id, ten, phong_id, ngan_sach)` — `phong_id` NULL: dự án chung, chưa giao phòng nào

| id | ten | phong_id | ngan_sach |
| --- | --- | --- | --- |
| 1 | Hệ thống CRM | 1 | 800000000 |
| 2 | Ứng dụng bán hàng | 1 | 600000000 |
| 3 | Chiến dịch Tết | 2 | 200000000 |
| 4 | Chuyển đổi số | NULL | 900000000 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
