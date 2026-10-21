# Khách mới theo tháng và số người đã mua

Với mỗi tháng có khách đăng ký (tháng dạng `YYYY-MM` của `ngay_dang_ky`), cho biết **số khách mới** đăng ký trong
tháng và **số người trong đó đã mua**: có ít nhất một đơn **không bị huỷ**.

Tháng có khách đăng ký mà chưa ai mua vẫn phải có dòng, với số người đã mua bằng 0. Điều kiện về đơn đặt ở `WHERE`
hay ở `ON` của `LEFT JOIN` cho kết quả khác nhau.

Kết quả có 3 cột: thang, khach_moi, da_mua. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`khach_hang(id, ho_ten, thanh_pho, ngay_dang_ky)` — `thanh_pho` NULL: chưa ghi

| id | ho_ten | thanh_pho | ngay_dang_ky |
| --- | --- | --- | --- |
| 1 | Nguyễn Văn An | Hà Nội | 2024-01-15 |
| 2 | Trần Thị Bình | Hồ Chí Minh | 2024-02-20 |
| 3 | Lê Minh Cường | Hà Nội | 2024-03-05 |
| 4 | Phạm Thu Dung | NULL | 2024-03-18 |
| 5 | Hoàng Văn Em | Đà Nẵng | 2024-04-02 |
| 6 | Vũ Thị Giang | Hồ Chí Minh | 2024-05-11 |
| 7 | Đặng Quốc Huy | NULL | 2024-06-25 |
| 8 | Bùi Ngọc Lan | Hà Nội | 2024-07-30 |

`don_hang(id, khach_hang_id, ngay_dat, trang_thai)` — `khach_hang_id` NULL: khách vãng lai; `trang_thai` là `moi`, `dang-giao`, `da-giao` hoặc `huy`

| id | khach_hang_id | ngay_dat | trang_thai |
| --- | --- | --- | --- |
| 1 | 1 | 2024-08-01 | da-giao |
| 2 | 2 | 2024-08-03 | da-giao |
| 3 | 1 | 2024-08-10 | huy |
| 4 | 3 | 2024-08-15 | dang-giao |
| 5 | NULL | 2024-08-20 | da-giao |
| 6 | 5 | 2024-09-01 | moi |
| 7 | 2 | 2024-09-05 | da-giao |
| 8 | 6 | 2024-09-12 | huy |
| 9 | 1 | 2024-09-20 | da-giao |
| 10 | 3 | 2024-10-01 | moi |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
