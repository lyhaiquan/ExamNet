# Dự án chưa có ai phân công

Liệt kê **mã và tên** các dự án **chưa có nhân viên nào** được phân công.

`du_an LEFT JOIN phan_cong` rồi giữ những dự án không ghép được dòng phân công nào.

Kết quả có 2 cột: id, ten. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`du_an(id, ten, phong_id, ngan_sach)` — `phong_id` NULL: dự án chung, chưa giao phòng nào

| id | ten | phong_id | ngan_sach |
| --- | --- | --- | --- |
| 1 | Hệ thống CRM | 1 | 800000000 |
| 2 | Ứng dụng bán hàng | 1 | 600000000 |
| 3 | Chiến dịch Tết | 2 | 200000000 |
| 4 | Chuyển đổi số | NULL | 900000000 |
| 5 | Kho dữ liệu | 1 | 300000000 |

`phan_cong(nhan_vien_id, du_an_id, so_gio)`

| nhan_vien_id | du_an_id | so_gio |
| --- | --- | --- |
| 2 | 1 | 120 |
| 3 | 1 | 160 |
| 4 | 2 | 140 |
| 3 | 2 | 40 |
| 5 | 3 | 100 |
| 6 | 3 | 80 |
| 7 | 3 | 60 |
| 2 | 4 | 30 |
| 1 | 4 | 20 |
| 8 | 4 | 50 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
