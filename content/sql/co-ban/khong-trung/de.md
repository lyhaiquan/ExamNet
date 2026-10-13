# Các thành phố có khách hàng

Liệt kê các **thành phố** có khách hàng, **mỗi thành phố một dòng**. Khách chưa ghi thành phố
(`thanh_pho` NULL) không tính.

Câu này dạy `DISTINCT`: nhiều khách cùng thành phố cho ra nhiều dòng giống hệt nhau; `SELECT DISTINCT` gộp các
dòng trùng thành một. `DISTINCT` coi mọi NULL là một giá trị, nên phải lọc NULL bằng `WHERE thanh_pho IS NOT NULL`
(không phải `<> NULL`).

Kết quả có 1 cột: thanh_pho. Thứ tự dòng không quan trọng.

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

Server chấm trên dữ liệu mẫu ở trên **và 3 bộ dữ liệu ẩn** (có NULL, dòng trùng, bảng rỗng): câu đúng phải đúng với mọi dữ liệu, không chỉ với dữ liệu nhìn thấy.
