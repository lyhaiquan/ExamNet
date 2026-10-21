# Khách đặt đơn trong cả quý 1 và quý 2

Liệt kê **mã và họ tên** các khách có đặt đơn trong **quý 1/2024** (01/01 đến hết 31/03) **và** cũng có đặt đơn
trong **quý 2/2024** (01/04 đến hết 30/06). Tính mọi đơn, kể cả đơn đã huỷ.

Chú ý các đơn đặt đúng ngày đầu hoặc cuối quý.

Kết quả có 2 cột: id, ho_ten. Thứ tự dòng không quan trọng.

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
| 11 | 1 | 2024-02-10 | da-giao |
| 12 | 1 | 2024-05-03 | da-giao |
| 13 | 2 | 2024-03-31 | da-giao |
| 14 | 3 | 2024-04-01 | huy |
| 15 | 3 | 2024-01-20 | moi |
| 16 | 2 | 2024-07-01 | da-giao |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
