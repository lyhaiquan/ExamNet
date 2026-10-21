# Sản phẩm kèm tên danh mục

Liệt kê **tên sản phẩm** kèm **tên danh mục** của sản phẩm đó.

Hai bảng đều có cột `ten`: viết rõ `s.ten`, `d.ten` (đặt bí danh cho bảng) để không nhầm.

Kết quả có 2 cột: ten, ten. Thứ tự dòng không quan trọng.

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

`danh_muc(id, ten, cha_id)` — `cha_id` NULL: danh mục gốc

| id | ten | cha_id |
| --- | --- | --- |
| 1 | Điện tử | NULL |
| 2 | Điện thoại | 1 |
| 3 | Máy tính xách tay | 1 |
| 4 | Sách | NULL |
| 5 | Sách lập trình | 4 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
