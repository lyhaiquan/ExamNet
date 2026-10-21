# Sản phẩm chưa từng được bán

Liệt kê **mã và tên** các sản phẩm **chưa từng xuất hiện** trong chi tiết đơn nào (đơn huỷ cũng tính là đã xuất
hiện).

Kết quả có 2 cột: id, ten. Thứ tự dòng không quan trọng.

## Bảng dùng trong bài

`san_pham(id, ten, danh_muc_id, gia, ton_kho)`

| id | ten | danh_muc_id | gia | ton_kho |
| --- | --- | --- | --- | --- |
| 1 | iPhone 15 | 2 | 22000000 | 5 |
| 2 | Galaxy S24 | 2 | 20000000 | 8 |
| 3 | Redmi Note 13 | 2 | 5000000 | 20 |
| 4 | MacBook Air M3 | 3 | 28000000 | 3 |
| 5 | ThinkPad E14 | 3 | 16000000 | 6 |
| 6 | Dell Inspiron 15 | 3 | 16000000 | 4 |
| 7 | Python cơ bản | 5 | 150000 | 50 |
| 8 | Cấu trúc dữ liệu và giải thuật | 5 | 180000 | 30 |
| 9 | Màn hình LG 27 inch | 1 | 16000000 | 7 |
| 10 | Sạc dự phòng | 1 | 600000 | 0 |
| 11 | Tai nghe Bluetooth | 1 | 900000 | 15 |
| 12 | Sách SQL nâng cao | 5 | 210000 | 0 |

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
