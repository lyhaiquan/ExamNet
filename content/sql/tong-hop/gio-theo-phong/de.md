# Tổng giờ dự án theo phòng

Với **mọi phòng**, tính **tổng số giờ** đã phân công trên các dự án mà phòng đó **phụ trách** (`du_an.phong_id`).
Phòng không phụ trách dự án nào, hoặc dự án chưa ai làm, có tổng giờ là **0** (không phải NULL).

Kết quả: mã phòng, tên phòng, tổng giờ.

Kết quả có 3 cột: id, ten, tong_gio. Thứ tự dòng không quan trọng.

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
