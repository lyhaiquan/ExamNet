# 5 sản phẩm đắt nhất

Liệt kê **mã, tên, giá** của **5 sản phẩm đắt nhất**. Cùng giá thì sản phẩm có **mã nhỏ hơn đứng trước**.

Câu này dạy `ORDER BY` và `LIMIT`. `ORDER BY gia DESC` sắp giá giảm dần; thêm `, id` (tăng dần) để các dòng cùng
giá có thứ tự xác định. `LIMIT 5` chỉ giữ 5 dòng đầu **sau khi đã sắp**. Thiếu khoá phụ `id` thì ba sản phẩm cùng
giá 16 000 000 ở ranh giới đứng theo thứ tự nào cũng được, và dòng thứ năm có thể khác đáp án.

Kết quả có 3 cột: id, ten, gia. **Thứ tự dòng được chấm.**

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

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
