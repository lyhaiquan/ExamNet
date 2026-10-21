# Sản phẩm có giá cao nhất

Liệt kê **mã, tên, giá** của sản phẩm có **giá cao nhất**. Nếu nhiều sản phẩm cùng giá cao nhất thì **in tất cả**.

Câu con một giá trị `(SELECT MAX(gia) FROM san_pham)` tính giá cao nhất trước, câu ngoài lọc những sản phẩm có
đúng giá đó. `ORDER BY gia DESC LIMIT 1` chỉ ra **một** dòng nên sai khi có nhiều sản phẩm cùng giá cao nhất.

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
| 11 | MacBook Pro M3 | 3 | 28000000 | 2 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
