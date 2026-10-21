# Khách đã mua cả sản phẩm 1 và 2

Liệt kê **mã và họ tên** các khách đã mua **cả** sản phẩm mã **1** **lẫn** sản phẩm mã **2** (có thể ở hai đơn khác
nhau; tính mọi đơn, kể cả đơn huỷ).

Một dòng chi tiết chỉ có một sản phẩm, nên `WHERE san_pham_id = 1 AND san_pham_id = 2` không bao giờ đúng. Lọc
`IN (1, 2)` rồi gộp theo khách và đếm số sản phẩm **khác nhau**.

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

`chi_tiet_don(don_hang_id, san_pham_id, so_luong, don_gia)` — thành tiền một dòng = `so_luong * don_gia`

| don_hang_id | san_pham_id | so_luong | don_gia |
| --- | --- | --- | --- |
| 1 | 1 | 1 | 22000000 |
| 1 | 9 | 1 | 16000000 |
| 2 | 7 | 2 | 150000 |
| 2 | 8 | 1 | 180000 |
| 3 | 4 | 1 | 28000000 |
| 4 | 3 | 2 | 5000000 |
| 5 | 10 | 3 | 600000 |
| 6 | 2 | 1 | 20000000 |
| 7 | 5 | 1 | 16000000 |
| 8 | 6 | 1 | 16000000 |
| 9 | 8 | 2 | 180000 |
| 10 | 7 | 1 | 150000 |
| 9 | 2 | 1 | 20000000 |
| 7 | 1 | 1 | 22000000 |
| 2 | 1 | 1 | 22000000 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
