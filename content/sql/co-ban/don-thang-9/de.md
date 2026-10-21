# Đơn hàng đặt trong tháng 9/2024

Liệt kê **mã đơn, mã khách, ngày đặt** của các đơn đặt trong **tháng 9 năm 2024** (từ 01/09 đến hết 30/09).

Ngày lưu dạng chuỗi `YYYY-MM-DD`. Cách gọn: `ngay_dat >= '2024-09-01' AND ngay_dat < '2024-10-01'`, hoặc dùng hàm
ngày của SQLite `strftime('%Y-%m', ngay_dat) = '2024-09'`. Đừng bỏ sót đơn đặt đúng ngày 01/09.

Kết quả có 3 cột: id, khach_hang_id, ngay_dat. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

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
