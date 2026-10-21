# Tỉ lệ đơn huỷ theo tháng

Với mỗi tháng có đơn hàng, tính **tỉ lệ phần trăm đơn bị huỷ** trong số đơn đặt trong tháng đó, **làm tròn 2 chữ
số** sau dấu phẩy. Tháng ghi dạng `YYYY-MM`: `strftime('%Y-%m', ngay_dat)`.

Cẩn thận phép chia số nguyên: trong SQLite `1 / 4` ra `0`. Nhân với `100.0` trước khi chia.

Kết quả có 2 cột: thang, ty_le_huy. Thứ tự dòng không quan trọng.

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
