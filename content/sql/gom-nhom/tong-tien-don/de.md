# Tổng tiền từng đơn hàng

Tính **tổng tiền** của từng đơn hàng: cộng thành tiền mọi dòng chi tiết của đơn đó. Thành tiền một dòng là
`so_luong * don_gia`.

Hàm gộp nhận được cả biểu thức: `SUM(so_luong * don_gia)` nhân từng dòng rồi mới cộng.

Kết quả có 2 cột: don_hang_id, tong_tien. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

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
