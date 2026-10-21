# Khách hàng họ Nguyễn

Liệt kê **mã và họ tên** các khách hàng **họ Nguyễn**. Họ là chữ đầu tiên của `ho_ten`.

`LIKE` so khớp mẫu: `%` thay cho một chuỗi bất kỳ (kể cả rỗng), `_` thay cho đúng một ký tự. Cẩn thận: khách tên
"Trần Nguyễn Hà" có chữ Nguyễn nhưng **không phải** họ Nguyễn.

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
| 9 | Trần Nguyễn Hà | Huế | 2024-08-05 |
| 10 | Nguyễn Thị Hồng | Hải Phòng | 2024-08-12 |

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
