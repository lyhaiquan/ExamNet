# Sản phẩm giá từ 100 000 đến 500 000 còn hàng

Liệt kê **mã, tên, giá** các sản phẩm có giá **từ 100 000 đến 500 000** (tính cả hai đầu) và **còn hàng**
(`ton_kho` lớn hơn 0).

`gia BETWEEN a AND b` nghĩa là `gia >= a AND gia <= b`: **lấy cả hai đầu mút**. Dữ liệu có sản phẩm giá đúng
100 000 và đúng 500 000.

Kết quả có 3 cột: id, ten, gia. Thứ tự dòng không quan trọng.

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
| 11 | Chuột không dây | 1 | 500000 | 12 |
| 12 | Lót chuột | 1 | 100000 | 9 |
| 13 | Bút trình chiếu | 1 | 450000 | 0 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
