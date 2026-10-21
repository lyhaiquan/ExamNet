# Sản phẩm bán được cả tháng 8 và tháng 9/2024

Liệt kê **mã sản phẩm** bán được trong **cả tháng 8/2024 lẫn tháng 9/2024**. Sản phẩm "bán được" trong một tháng
nếu nó có trong chi tiết của một đơn **đặt trong tháng đó** và đơn **không bị huỷ** (`trang_thai <> 'huy'`).

Viết hai câu (một câu cho mỗi tháng, đều `JOIN` `chi_tiet_don` với `don_hang`) rồi lấy phần chung bằng
`INTERSECT`.

Kết quả có 1 cột: san_pham_id. Thứ tự dòng không quan trọng.

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

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
